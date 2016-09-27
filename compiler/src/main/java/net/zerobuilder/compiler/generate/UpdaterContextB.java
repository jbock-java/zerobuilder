package net.zerobuilder.compiler.generate;

import com.google.common.base.Function;
import com.google.common.collect.ImmutableList;
import com.squareup.javapoet.CodeBlock;
import com.squareup.javapoet.FieldSpec;
import com.squareup.javapoet.MethodSpec;
import com.squareup.javapoet.ParameterSpec;
import com.squareup.javapoet.ParameterizedTypeName;
import net.zerobuilder.compiler.generate.DtoBeanGoalContext.BeanGoalContext;
import net.zerobuilder.compiler.generate.DtoStep.AbstractBeanStep;
import net.zerobuilder.compiler.generate.DtoStep.AccessorPairStep;
import net.zerobuilder.compiler.generate.DtoStep.BeanStepCases;
import net.zerobuilder.compiler.generate.DtoStep.LoneGetterStep;

import static com.squareup.javapoet.MethodSpec.methodBuilder;
import static com.squareup.javapoet.WildcardTypeName.subtypeOf;
import static javax.lang.model.element.Modifier.PUBLIC;
import static net.zerobuilder.compiler.Utilities.nullCheck;
import static net.zerobuilder.compiler.analyse.DtoBeanParameter.beanParameterName;
import static net.zerobuilder.compiler.analyse.ProjectionValidatorB.ITERABLE;
import static net.zerobuilder.compiler.generate.UpdaterContext.updaterType;

final class UpdaterContextB {

  static final Function<BeanGoalContext, ImmutableList<FieldSpec>> fields
      = new Function<BeanGoalContext, ImmutableList<FieldSpec>>() {
    @Override
    public ImmutableList<FieldSpec> apply(BeanGoalContext goal) {
      return ImmutableList.of(goal.field);
    }
  };

  static final Function<BeanGoalContext, ImmutableList<MethodSpec>> updateMethods
      = new Function<BeanGoalContext, ImmutableList<MethodSpec>>() {
    @Override
    public ImmutableList<MethodSpec> apply(BeanGoalContext goal) {
      ImmutableList.Builder<MethodSpec> builder = ImmutableList.builder();
      BeanStepCases<ImmutableList<MethodSpec>> handler = updateMethodsCases(goal);
      for (AbstractBeanStep step : goal.steps) {
        builder.addAll(step.acceptBean(handler));
      }
      return builder.build();
    }
  };

  private static BeanStepCases<ImmutableList<MethodSpec>> updateMethodsCases(final BeanGoalContext goal) {
    return new BeanStepCases<ImmutableList<MethodSpec>>() {
      @Override
      public ImmutableList<MethodSpec> accessorPair(AccessorPairStep step) {
        return ImmutableList.of(regularUpdater(goal, step));
      }
      @Override
      public ImmutableList<MethodSpec> loneGetter(LoneGetterStep step) {
        return collectionUpdaters(goal, step);
      }
    };
  }

  private static MethodSpec regularUpdater(BeanGoalContext goal, AccessorPairStep step) {
    String name = step.accessorPair.accept(beanParameterName);
    ParameterSpec parameter = step.parameter();
    return methodBuilder(name)
        .returns(goal.accept(updaterType))
        .addParameter(parameter)
        .addStatement("this.$N.$L($N)",
            goal.field, step.setter, parameter)
        .addStatement("return this")
        .addModifiers(PUBLIC)
        .build();
  }

  private static ImmutableList<MethodSpec> collectionUpdaters(BeanGoalContext goal, LoneGetterStep step) {
    ImmutableList.Builder<MethodSpec> builder = ImmutableList.builder();
    builder.add(iterateCollection(goal, step));
    builder.add(emptyCollection(goal, step));
    return builder.build();
  }

  private static MethodSpec iterateCollection(BeanGoalContext goal, LoneGetterStep step) {
    ParameterizedTypeName iterable = ParameterizedTypeName.get(ITERABLE,
        subtypeOf(step.loneGetter.iterationType()));
    String name = step.loneGetter.accept(beanParameterName);
    ParameterSpec parameter = ParameterSpec.builder(iterable, name).build();
    ParameterSpec iterationVar = step.loneGetter.iterationVar(parameter);
    return methodBuilder(name)
        .returns(goal.accept(updaterType))
        .addParameter(parameter)
        .addCode(nullCheck(name, name))
        .addCode(clearCollection(goal, step))
        .beginControlFlow("for ($T $N : $N)", iterationVar.type, iterationVar, name)
        .addStatement("this.$N.$N().add($N)",
            goal.field, step.loneGetter.getter, iterationVar)
        .endControlFlow()
        .addStatement("return this")
        .addModifiers(PUBLIC)
        .build();
  }

  private static MethodSpec emptyCollection(BeanGoalContext goal, LoneGetterStep step) {
    return methodBuilder(step.emptyMethod)
        .returns(goal.accept(updaterType))
        .addCode(clearCollection(goal, step))
        .addStatement("return this")
        .addModifiers(PUBLIC)
        .build();
  }

  private static CodeBlock clearCollection(BeanGoalContext goal, LoneGetterStep step) {
    return CodeBlock.builder().addStatement("this.$N.$N().clear()",
        goal.field, step.loneGetter.getter).build();
  }

  private UpdaterContextB() {
    throw new UnsupportedOperationException("no instances");
  }
}
