package net.zerobuilder.compiler.generate;

import com.google.common.base.Function;
import com.google.common.base.Optional;
import com.google.common.collect.ImmutableList;
import com.squareup.javapoet.CodeBlock;
import com.squareup.javapoet.FieldSpec;
import com.squareup.javapoet.MethodSpec;
import com.squareup.javapoet.ParameterSpec;
import com.squareup.javapoet.ParameterizedTypeName;
import com.squareup.javapoet.TypeName;
import net.zerobuilder.compiler.generate.DtoBeanGoalContext.BeanGoalContext;
import net.zerobuilder.compiler.generate.DtoBeanStep.AbstractBeanStep;
import net.zerobuilder.compiler.generate.DtoBeanStep.AccessorPairStep;
import net.zerobuilder.compiler.generate.DtoBeanStep.BeanStepCases;
import net.zerobuilder.compiler.generate.DtoBeanStep.LoneGetterStep;
import net.zerobuilder.compiler.generate.DtoStep.EmptyOption;

import static com.google.common.base.Optional.absent;
import static com.google.common.base.Optional.presentInstances;
import static com.google.common.collect.ImmutableList.of;
import static com.squareup.javapoet.MethodSpec.methodBuilder;
import static com.squareup.javapoet.WildcardTypeName.subtypeOf;
import static javax.lang.model.element.Modifier.PUBLIC;
import static net.zerobuilder.compiler.Utilities.ClassNames.ITERABLE;
import static net.zerobuilder.compiler.Utilities.nullCheck;
import static net.zerobuilder.compiler.Utilities.parameterSpec;
import static net.zerobuilder.compiler.analyse.DtoBeanParameter.beanParameterName;
import static net.zerobuilder.compiler.generate.DtoBeanStep.asFunction;
import static net.zerobuilder.compiler.generate.UpdaterContext.updaterType;

final class UpdaterContextB {

  static final Function<BeanGoalContext, ImmutableList<FieldSpec>> fields
      = new Function<BeanGoalContext, ImmutableList<FieldSpec>>() {
    @Override
    public ImmutableList<FieldSpec> apply(BeanGoalContext goal) {
      return of(goal.field);
    }
  };

  static final Function<BeanGoalContext, ImmutableList<MethodSpec>> updateMethods
      = new Function<BeanGoalContext, ImmutableList<MethodSpec>>() {
    @Override
    public ImmutableList<MethodSpec> apply(BeanGoalContext goal) {
      ImmutableList.Builder<MethodSpec> builder = ImmutableList.builder();
      Function<AbstractBeanStep, ImmutableList<MethodSpec>> updateMethods = stepToMethods(goal);
      for (AbstractBeanStep step : goal.steps) {
        builder.addAll(updateMethods.apply(step));
      }
      return builder.build();
    }
  };

  private static Function<AbstractBeanStep, ImmutableList<MethodSpec>>
  stepToMethods(final BeanGoalContext goal) {
    return asFunction(new BeanStepCases<ImmutableList<MethodSpec>>() {
      @Override
      public ImmutableList<MethodSpec> accessorPair(AccessorPairStep step) {
        return regularMethods(step, goal);
      }
      @Override
      public ImmutableList<MethodSpec> loneGetter(LoneGetterStep step) {
        return collectionUpdaters(goal, step);
      }
    });
  }

  private static ImmutableList<MethodSpec> regularMethods(AccessorPairStep step, BeanGoalContext goal) {
    ImmutableList.Builder<MethodSpec> builder = ImmutableList.builder();
    builder.add(normalUpdate(goal, step));
    builder.addAll(presentInstances(of(regularEmptyCollection(goal, step))));
    return builder.build();
  }

  private static Optional<MethodSpec> regularEmptyCollection(BeanGoalContext goal, AccessorPairStep step) {
    Optional<EmptyOption> maybeEmptyOption = step.emptyOption();
    if (!maybeEmptyOption.isPresent()) {
      return absent();
    }
    EmptyOption emptyOption = maybeEmptyOption.get();
    TypeName type = step.accessorPair.type;
    String name = step.accessorPair.accept(beanParameterName);
    ParameterSpec emptyColl = parameterSpec(type, name);
    return Optional.of(methodBuilder(emptyOption.name)
        .returns(updaterType.apply(goal))
        .addStatement("$T $N = $L", emptyColl.type, emptyColl, emptyOption.initializer)
        .addStatement("this.$N.$L($N)",
            goal.field, step.setter, emptyColl)
        .addStatement("return this")
        .addModifiers(PUBLIC)
        .build());
  }

  private static MethodSpec normalUpdate(BeanGoalContext goal, AccessorPairStep step) {
    String name = step.accessorPair.accept(beanParameterName);
    ParameterSpec parameter = step.parameter();
    return methodBuilder(name)
        .returns(updaterType.apply(goal))
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
    builder.add(loneGetterEmptyCollection(goal, step));
    return builder.build();
  }

  private static MethodSpec iterateCollection(BeanGoalContext goal, LoneGetterStep step) {
    ParameterizedTypeName iterable = ParameterizedTypeName.get(ITERABLE,
        subtypeOf(step.loneGetter.iterationType()));
    String name = step.loneGetter.accept(beanParameterName);
    ParameterSpec parameter = parameterSpec(iterable, name);
    ParameterSpec iterationVar = step.loneGetter.iterationVar(parameter);
    return methodBuilder(name)
        .returns(updaterType.apply(goal))
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

  private static MethodSpec loneGetterEmptyCollection(BeanGoalContext goal, LoneGetterStep step) {
    return methodBuilder(step.emptyMethod)
        .returns(updaterType.apply(goal))
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
