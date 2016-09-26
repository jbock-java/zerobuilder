package net.zerobuilder.compiler.generate;

import com.google.common.base.Function;
import com.google.common.collect.ImmutableList;
import com.squareup.javapoet.ClassName;
import com.squareup.javapoet.CodeBlock;
import com.squareup.javapoet.FieldSpec;
import com.squareup.javapoet.MethodSpec;
import com.squareup.javapoet.ParameterSpec;
import com.squareup.javapoet.ParameterizedTypeName;
import com.squareup.javapoet.TypeName;
import net.zerobuilder.compiler.generate.DtoGoal.BeanGoalContext;
import net.zerobuilder.compiler.generate.DtoStep.BeanStep;

import static com.google.common.collect.Iterables.getLast;
import static com.squareup.javapoet.MethodSpec.methodBuilder;
import static com.squareup.javapoet.WildcardTypeName.subtypeOf;
import static javax.lang.model.element.Modifier.PUBLIC;
import static net.zerobuilder.compiler.Utilities.nullCheck;
import static net.zerobuilder.compiler.Utilities.parameterSpec;
import static net.zerobuilder.compiler.generate.StepContext.iterationVarNullCheck;
import static net.zerobuilder.compiler.generate.StepContext.nullCheck;

final class BuilderContextB {

  static final Function<BeanGoalContext, ImmutableList<FieldSpec>> fields
      = new Function<BeanGoalContext, ImmutableList<FieldSpec>>() {
    @Override
    public ImmutableList<FieldSpec> apply(BeanGoalContext goal) {
      return ImmutableList.of(goal.field);
    }
  };

  static final Function<BeanGoalContext, ImmutableList<MethodSpec>> stepsButLast
      = new Function<BeanGoalContext, ImmutableList<MethodSpec>>() {
    @Override
    public ImmutableList<MethodSpec> apply(BeanGoalContext goal) {
      ImmutableList.Builder<MethodSpec> builder = ImmutableList.builder();
      CodeBlock finalBlock = CodeBlock.builder().addStatement("return this").build();
      for (BeanStep step : goal.steps.subList(0, goal.steps.size() - 1)) {
        if (step.validParameter.collectionType.isPresent()) {
          builder.addAll(collectionMethods(step, goal, finalBlock));
        } else {
          builder.add(regularStep(step, goal, finalBlock));
        }
      }
      return builder.build();
    }
  };

  static final Function<BeanGoalContext, ImmutableList<MethodSpec>> lastStep
      = new Function<BeanGoalContext, ImmutableList<MethodSpec>>() {
    @Override
    public ImmutableList<MethodSpec> apply(BeanGoalContext goal) {
      BeanStep step = getLast(goal.steps);
      if (step.validParameter.collectionType.isPresent()) {
        return collectionMethods(step, goal, invoke.apply(goal));
      } else {
        return ImmutableList.of(regularStep(step, goal, invoke.apply(goal)));
      }
    }
  };

  private static ImmutableList<MethodSpec> collectionMethods(BeanStep step, BeanGoalContext goal, CodeBlock finalBlock) {
    MethodSpec fromIterable = iterateCollection(step, goal, finalBlock);
    MethodSpec fromEmpty = emptyCollection(step, finalBlock);
    ImmutableList.Builder<MethodSpec> builder = ImmutableList.builder();
    builder.add(fromIterable, fromEmpty);
    if (step.validParameter.collectionType.allowShortcut) {
      builder.add(singletonCollection(step, goal, finalBlock));
    }
    return builder.build();
  }

  private static MethodSpec emptyCollection(BeanStep step, CodeBlock finalBlock) {
    String name = step.validParameter.name;
    return methodBuilder(name)
        .addAnnotation(Override.class)
        .returns(step.nextType)
        .addCode(finalBlock)
        .addModifiers(PUBLIC)
        .build();
  }

  private static MethodSpec iterateCollection(BeanStep step,
                                              BeanGoalContext goal,
                                              CodeBlock finalBlock) {
    String name = step.validParameter.name;
    ParameterizedTypeName iterable = ParameterizedTypeName.get(ClassName.get(Iterable.class),
        subtypeOf(step.validParameter.collectionType.getType()));
    ParameterSpec parameter = parameterSpec(iterable, name);
    ParameterSpec iterationVar = step.validParameter.collectionType.get(parameter);
    return methodBuilder(step.validParameter.name)
        .addAnnotation(Override.class)
        .returns(step.nextType)
        .addParameter(parameter)
        .addCode(nullCheck(parameter))
        .beginControlFlow("for ($T $N : $N)",
            iterationVar.type, iterationVar, parameter)
        .addCode(iterationVarNullCheck(step, parameter))
        .addStatement("this.$N.$L().add($N)", goal.field,
            step.validParameter.getter, iterationVar)
        .endControlFlow()
        .addCode(finalBlock)
        .addModifiers(PUBLIC)
        .build();
  }

  private static MethodSpec singletonCollection(BeanStep step, BeanGoalContext goal, CodeBlock finalBlock) {
    String name = step.validParameter.name;
    TypeName type = step.validParameter.collectionType.getType();
    ParameterSpec parameter = parameterSpec(type, name);
    return methodBuilder(name)
        .addAnnotation(Override.class)
        .returns(step.nextType)
        .addParameter(parameter)
        .addCode(step.accept(nullCheck))
        .addStatement("this.$N.$L().add($N)", goal.field,
            step.validParameter.getter, parameter)
        .addCode(finalBlock)
        .addModifiers(PUBLIC)
        .build();
  }

  private static MethodSpec regularStep(BeanStep step, BeanGoalContext goal, CodeBlock finalBlock) {
    return methodBuilder(step.validParameter.name)
        .addAnnotation(Override.class)
        .addParameter(step.parameter)
        .addModifiers(PUBLIC)
        .returns(step.nextType)
        .addCode(step.accept(nullCheck))
        .addStatement("this.$N.$L($N)", goal.field, step.setter, step.parameter)
        .addCode(finalBlock).build();
  }

  static final Function<BeanGoalContext, CodeBlock> invoke
      = new Function<BeanGoalContext, CodeBlock>() {
    @Override
    public CodeBlock apply(BeanGoalContext goal) {
      return CodeBlock.builder()
          .addStatement("return this.$N", goal.field)
          .build();
    }
  };

  private BuilderContextB() {
    throw new UnsupportedOperationException("no instances");
  }
}
