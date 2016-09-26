package net.zerobuilder.compiler.generate;

import com.google.common.base.Function;
import com.google.common.collect.ImmutableList;
import com.squareup.javapoet.CodeBlock;
import com.squareup.javapoet.FieldSpec;
import com.squareup.javapoet.MethodSpec;
import com.squareup.javapoet.ParameterSpec;
import com.squareup.javapoet.ParameterizedTypeName;
import com.squareup.javapoet.TypeName;
import net.zerobuilder.compiler.generate.DtoBeanGoalContext.BeanGoalContext;
import net.zerobuilder.compiler.generate.DtoStep.AbstractBeanStep;
import net.zerobuilder.compiler.generate.DtoStep.AccessorPairStep;
import net.zerobuilder.compiler.generate.DtoStep.BeanStepCases;
import net.zerobuilder.compiler.generate.DtoStep.LoneGetterStep;

import static com.google.common.collect.Iterables.getLast;
import static com.squareup.javapoet.MethodSpec.methodBuilder;
import static com.squareup.javapoet.WildcardTypeName.subtypeOf;
import static javax.lang.model.element.Modifier.PUBLIC;
import static net.zerobuilder.compiler.Utilities.nullCheck;
import static net.zerobuilder.compiler.Utilities.parameterSpec;
import static net.zerobuilder.compiler.analyse.ProjectionValidatorB.ITERABLE;
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
      BeanStepCases<ImmutableList<MethodSpec>> stepHandler = handleStep(goal, finalBlock);
      for (AbstractBeanStep step : goal.steps.subList(0, goal.steps.size() - 1)) {
        builder.addAll(step.acceptBean(stepHandler));
      }
      return builder.build();
    }
  };

  private static BeanStepCases<ImmutableList<MethodSpec>> handleStep(final BeanGoalContext goal, final CodeBlock finalBlock) {
    return new BeanStepCases<ImmutableList<MethodSpec>>() {
      @Override
      public ImmutableList<MethodSpec> accessorPair(AccessorPairStep step) {
        return ImmutableList.of(regularStep(step, goal, finalBlock));
      }
      @Override
      public ImmutableList<MethodSpec> loneGetter(LoneGetterStep step) {
        return collectionMethods(step, goal, finalBlock);
      }
    };
  }

  static final Function<BeanGoalContext, ImmutableList<MethodSpec>> lastStep
      = new Function<BeanGoalContext, ImmutableList<MethodSpec>>() {
    @Override
    public ImmutableList<MethodSpec> apply(BeanGoalContext goal) {
      BeanStepCases<ImmutableList<MethodSpec>> handler = handleStep(goal, invoke.apply(goal));
      return getLast(goal.steps).acceptBean(handler);
    }
  };

  private static ImmutableList<MethodSpec> collectionMethods(LoneGetterStep step, BeanGoalContext goal, CodeBlock finalBlock) {
    MethodSpec fromIterable = iterateCollection(step, goal, finalBlock);
    MethodSpec fromEmpty = emptyCollection(step, finalBlock);
    ImmutableList.Builder<MethodSpec> builder = ImmutableList.builder();
    builder.add(fromIterable, fromEmpty);
    if (step.loneGetter.allowShortcut) {
      builder.add(singletonCollection(step, goal, finalBlock));
    }
    return builder.build();
  }

  private static MethodSpec emptyCollection(LoneGetterStep step, CodeBlock finalBlock) {
    String name = step.loneGetter.name;
    return methodBuilder(name)
        .addAnnotation(Override.class)
        .returns(step.nextType)
        .addCode(finalBlock)
        .addModifiers(PUBLIC)
        .build();
  }

  private static MethodSpec iterateCollection(LoneGetterStep step,
                                              BeanGoalContext goal,
                                              CodeBlock finalBlock) {
    String name = step.loneGetter.name;
    ParameterizedTypeName iterable = ParameterizedTypeName.get(ITERABLE,
        subtypeOf(step.loneGetter.iterationType()));
    ParameterSpec parameter = parameterSpec(iterable, name);
    ParameterSpec iterationVar = step.loneGetter.iterationVar(parameter);
    return methodBuilder(step.loneGetter.name)
        .addAnnotation(Override.class)
        .returns(step.nextType)
        .addParameter(parameter)
        .addCode(nullCheck(parameter))
        .beginControlFlow("for ($T $N : $N)",
            iterationVar.type, iterationVar, parameter)
        .addCode(iterationVarNullCheck(step, parameter))
        .addStatement("this.$N.$L().add($N)", goal.field,
            step.loneGetter.getter, iterationVar)
        .endControlFlow()
        .addCode(finalBlock)
        .addModifiers(PUBLIC)
        .build();
  }

  private static MethodSpec singletonCollection(LoneGetterStep step, BeanGoalContext goal, CodeBlock finalBlock) {
    String name = step.loneGetter.name;
    TypeName type = step.loneGetter.iterationType();
    ParameterSpec parameter = parameterSpec(type, name);
    return methodBuilder(name)
        .addAnnotation(Override.class)
        .returns(step.nextType)
        .addParameter(parameter)
        .addCode(step.accept(nullCheck))
        .addStatement("this.$N.$L().add($N)", goal.field,
            step.loneGetter.getter, parameter)
        .addCode(finalBlock)
        .addModifiers(PUBLIC)
        .build();
  }

  private static MethodSpec regularStep(AccessorPairStep step, BeanGoalContext goal, CodeBlock finalBlock) {
    ParameterSpec parameter = step.parameter();
    return methodBuilder(step.accessorPair.name)
        .addAnnotation(Override.class)
        .addParameter(parameter)
        .addModifiers(PUBLIC)
        .returns(step.nextType)
        .addCode(step.accept(nullCheck))
        .addStatement("this.$N.$L($N)", goal.field, step.setter, parameter)
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
