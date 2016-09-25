package net.zerobuilder.compiler.generate;

import com.google.common.base.Function;
import com.google.common.collect.ImmutableList;
import com.squareup.javapoet.CodeBlock;
import com.squareup.javapoet.FieldSpec;
import com.squareup.javapoet.MethodSpec;
import com.squareup.javapoet.ParameterizedTypeName;
import com.squareup.javapoet.TypeName;
import net.zerobuilder.compiler.generate.DtoGoal.BeanGoalContext;
import net.zerobuilder.compiler.generate.StepContext.BeansStep;

import static com.squareup.javapoet.MethodSpec.methodBuilder;
import static com.squareup.javapoet.WildcardTypeName.subtypeOf;
import static javax.lang.model.element.Modifier.PUBLIC;
import static net.zerobuilder.compiler.Utilities.iterationVarName;
import static net.zerobuilder.compiler.Utilities.nullCheck;
import static net.zerobuilder.compiler.Utilities.parameterSpec;
import static net.zerobuilder.compiler.analyse.ProjectionValidatorB.ITERABLE;
import static net.zerobuilder.compiler.generate.StepContext.maybeIterationNullCheck;
import static net.zerobuilder.compiler.generate.StepContext.maybeNullCheck;
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
      for (BeansStep step : goal.steps) {
        if (step.validParameter.collectionType.isPresent()) {
          builder.addAll(collectionUpdaters(goal, step));
        } else {
          builder.add(regularUpdater(goal, step));
        }
      }
      return builder.build();
    }
  };

  private static MethodSpec regularUpdater(BeanGoalContext goal, BeansStep step) {
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

  private static ImmutableList<MethodSpec> collectionUpdaters(BeanGoalContext goal, BeansStep step) {
    ImmutableList.Builder<MethodSpec> builder = ImmutableList.builder();
    builder.add(iterateCollection(goal, step));
    builder.add(emptyCollection(goal, step));
    if (step.validParameter.collectionType.allowShortcut) {
      builder.add(singletonCollection(goal, step));
    }
    return builder.build();
  }

  private static MethodSpec iterateCollection(BeanGoalContext goal, BeansStep step) {
    TypeName collectionType = step.validParameter.collectionType.get();
    ParameterizedTypeName iterable = ParameterizedTypeName.get(ITERABLE, subtypeOf(collectionType));
    String name = step.validParameter.name;
    return methodBuilder(name)
        .returns(goal.accept(typeName))
        .addParameter(parameterSpec(iterable, name))
        .addCode(nullCheck(name, name))
        .addCode(clearCollection(goal, step))
        .beginControlFlow("for ($T $N : $N)", collectionType, iterationVarName, name)
        .addCode(step.accept(maybeIterationNullCheck))
        .addStatement("this.$N.$N().add($N)",
            goal.field,
            step.validParameter.getter, iterationVarName)
        .endControlFlow()
        .addStatement("return this")
        .addModifiers(PUBLIC)
        .build();
  }

  private static MethodSpec emptyCollection(BeanGoalContext goal, BeansStep step) {
    String name = step.validParameter.name;
    return methodBuilder(name)
        .returns(goal.accept(typeName))
        .addCode(clearCollection(goal, step))
        .addStatement("return this")
        .addModifiers(PUBLIC)
        .build();
  }

  private static MethodSpec singletonCollection(BeanGoalContext goal, BeansStep step) {
    TypeName collectionType = step.validParameter.collectionType.get();
    String name = step.validParameter.name;
    return methodBuilder(name)
        .returns(goal.accept(typeName))
        .addParameter(parameterSpec(collectionType, name))
        .addCode(step.accept(maybeNullCheck))
        .addCode(clearCollection(goal, step))
        .addStatement("this.$N.$N().add($N)",
            goal.field, step.validParameter.getter, name)
        .addStatement("return this")
        .addModifiers(PUBLIC)
        .build();
  }

  private static CodeBlock clearCollection(BeanGoalContext goal, BeansStep step) {
    return CodeBlock.builder().addStatement("this.$N.$N().clear()",
        goal.field, step.validParameter.getter).build();
  }

  private UpdaterContextB() {
    throw new UnsupportedOperationException("no instances");
  }
}
