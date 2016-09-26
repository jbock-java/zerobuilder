package net.zerobuilder.compiler.generate;

import com.google.common.base.Function;
import com.google.common.collect.ImmutableList;
import com.squareup.javapoet.CodeBlock;
import com.squareup.javapoet.FieldSpec;
import com.squareup.javapoet.MethodSpec;
import com.squareup.javapoet.ParameterSpec;
import com.squareup.javapoet.ParameterizedTypeName;
import com.squareup.javapoet.TypeName;
import net.zerobuilder.compiler.generate.DtoGoal.BeanGoalContext;
import net.zerobuilder.compiler.generate.DtoStep.BeanStep;

import static com.squareup.javapoet.MethodSpec.methodBuilder;
import static com.squareup.javapoet.WildcardTypeName.subtypeOf;
import static javax.lang.model.element.Modifier.PUBLIC;
import static net.zerobuilder.compiler.Utilities.nullCheck;
import static net.zerobuilder.compiler.Utilities.parameterSpec;
import static net.zerobuilder.compiler.analyse.ProjectionValidatorB.ITERABLE;
import static net.zerobuilder.compiler.generate.StepContext.iterationVarNullCheck;
import static net.zerobuilder.compiler.generate.StepContext.nullCheck;
import static net.zerobuilder.compiler.generate.UpdaterContext.typeName;

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
      for (BeanStep step : goal.steps) {
        if (step.validParameter.collectionType.isPresent()) {
          builder.addAll(collectionUpdaters(goal, step));
        } else {
          builder.add(regularUpdater(goal, step));
        }
      }
      return builder.build();
    }
  };

  private static MethodSpec regularUpdater(BeanGoalContext goal, BeanStep step) {
    String name = step.validParameter.name;
    return methodBuilder(name)
        .returns(goal.accept(typeName))
        .addParameter(step.parameter)
        .addStatement("this.$N.$L($N)",
            goal.field, step.setter, step.parameter)
        .addStatement("return this")
        .addModifiers(PUBLIC)
        .build();
  }

  private static ImmutableList<MethodSpec> collectionUpdaters(BeanGoalContext goal, BeanStep step) {
    ImmutableList.Builder<MethodSpec> builder = ImmutableList.builder();
    builder.add(iterateCollection(goal, step));
    builder.add(emptyCollection(goal, step));
    if (step.validParameter.collectionType.allowShortcut) {
      builder.add(singletonCollection(goal, step));
    }
    return builder.build();
  }

  private static MethodSpec iterateCollection(BeanGoalContext goal, BeanStep step) {
    ParameterizedTypeName iterable = ParameterizedTypeName.get(ITERABLE,
        subtypeOf(step.validParameter.collectionType.getType()));
    String name = step.validParameter.name;
    ParameterSpec parameter = parameterSpec(iterable, name);
    ParameterSpec iterationVar = step.validParameter.collectionType.get(parameter);
    return methodBuilder(name)
        .returns(goal.accept(typeName))
        .addParameter(parameter)
        .addCode(nullCheck(name, name))
        .addCode(clearCollection(goal, step))
        .beginControlFlow("for ($T $N : $N)", iterationVar.type, iterationVar, name)
        .addCode(iterationVarNullCheck(step, parameter))
        .addStatement("this.$N.$N().add($N)",
            goal.field, step.validParameter.getter, iterationVar)
        .endControlFlow()
        .addStatement("return this")
        .addModifiers(PUBLIC)
        .build();
  }

  private static MethodSpec emptyCollection(BeanGoalContext goal, BeanStep step) {
    String name = step.validParameter.name;
    return methodBuilder(name)
        .returns(goal.accept(typeName))
        .addCode(clearCollection(goal, step))
        .addStatement("return this")
        .addModifiers(PUBLIC)
        .build();
  }

  private static MethodSpec singletonCollection(BeanGoalContext goal, BeanStep step) {
    String name = step.validParameter.name;
    TypeName type = step.validParameter.collectionType.getType();
    ParameterSpec parameter = parameterSpec(type, step.validParameter.name);
    return methodBuilder(name)
        .returns(goal.accept(typeName))
        .addParameter(parameter)
        .addCode(step.accept(nullCheck))
        .addCode(clearCollection(goal, step))
        .addStatement("this.$N.$N().add($N)",
            goal.field, step.validParameter.getter, name)
        .addStatement("return this")
        .addModifiers(PUBLIC)
        .build();
  }

  private static CodeBlock clearCollection(BeanGoalContext goal, BeanStep step) {
    return CodeBlock.builder().addStatement("this.$N.$N().clear()",
        goal.field, step.validParameter.getter).build();
  }

  private UpdaterContextB() {
    throw new UnsupportedOperationException("no instances");
  }
}
