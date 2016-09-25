package net.zerobuilder.compiler.generate;

import com.google.common.base.Function;
import com.google.common.base.Joiner;
import com.google.common.collect.ImmutableList;
import com.squareup.javapoet.CodeBlock;
import com.squareup.javapoet.FieldSpec;
import com.squareup.javapoet.MethodSpec;
import com.squareup.javapoet.TypeName;
import net.zerobuilder.compiler.generate.DtoGoal.RegularGoalContext;
import net.zerobuilder.compiler.generate.StepContext.RegularStep;

import static com.google.common.collect.Iterables.getLast;
import static com.squareup.javapoet.MethodSpec.methodBuilder;
import static com.squareup.javapoet.TypeName.VOID;
import static javax.lang.model.element.Modifier.PUBLIC;
import static net.zerobuilder.compiler.analyse.GoalContextFactory.GoalKind.INSTANCE_METHOD;
import static net.zerobuilder.compiler.generate.StepContext.maybeNullCheck;

final class BuilderContextV {

  static final Function<RegularGoalContext, ImmutableList<FieldSpec>> fields
      = new Function<RegularGoalContext, ImmutableList<FieldSpec>>() {
    @Override
    public ImmutableList<FieldSpec> apply(RegularGoalContext goal) {
      ImmutableList.Builder<FieldSpec> builder = ImmutableList.builder();
      if (goal.goal.kind == INSTANCE_METHOD) {
        builder.add(goal.builders.field);
      }
      for (RegularStep step : goal.steps.subList(0, goal.steps.size() - 1)) {
        builder.add(step.field);
      }
      return builder.build();
    }
  };

  static final Function<RegularGoalContext, ImmutableList<MethodSpec>> stepsButLast
      = new Function<RegularGoalContext, ImmutableList<MethodSpec>>() {
    @Override
    public ImmutableList<MethodSpec> apply(RegularGoalContext goal) {
      ImmutableList.Builder<MethodSpec> builder = ImmutableList.builder();
      for (RegularStep step : goal.steps.subList(0, goal.steps.size() - 1)) {
        CodeBlock finalBlock = CodeBlock.builder()
            .addStatement("this.$N = $N", step.field, step.parameter)
            .addStatement("return this")
            .build();
        builder.add(regularStep(step, finalBlock, ImmutableList.<TypeName>of()));
      }
      return builder.build();
    }
  };

  static final Function<RegularGoalContext, ImmutableList<MethodSpec>> lastStep
      = new Function<RegularGoalContext, ImmutableList<MethodSpec>>() {
    @Override
    public ImmutableList<MethodSpec> apply(RegularGoalContext goal) {
      RegularStep parameter = getLast(goal.steps);
      return ImmutableList.of(regularStep(parameter, invoke.apply(goal), goal.thrownTypes));
    }
  };

  private static MethodSpec regularStep(RegularStep step,
                                        CodeBlock finalBlock, ImmutableList<TypeName> thrownTypes) {
    return methodBuilder(step.validParameter.name)
        .addAnnotation(Override.class)
        .addParameter(step.parameter)
        .addExceptions(thrownTypes)
        .addModifiers(PUBLIC)
        .returns(step.nextType)
        .addCode(step.accept(maybeNullCheck))
        .addCode(finalBlock).build();
  }

  static final Function<RegularGoalContext, CodeBlock> invoke
      = new Function<RegularGoalContext, CodeBlock>() {
    @Override
    public CodeBlock apply(RegularGoalContext goal) {
      CodeBlock parameters = CodeBlock.of(Joiner.on(", ").join(goal.goal.parameterNames));
      CodeBlock.Builder builder = CodeBlock.builder();
      builder.add(CodeBlock.of(VOID.equals(goal.goal.goalType) ? "" : "return "));
      switch (goal.goal.kind) {
        case CONSTRUCTOR:
          return builder
              .addStatement("new $T($L)",
                  goal.goal.goalType, parameters).build();
        case INSTANCE_METHOD:
          return builder.addStatement("$N.$N($L)",
              goal.builders.field, goal.goal.methodName, parameters).build();
        case STATIC_METHOD:
          return builder.addStatement("$T.$N($L)",
              goal.builders.type, goal.goal.methodName, parameters).build();
        default:
          throw new IllegalStateException("unknown kind: " + goal.goal.kind);
      }
    }
  };

  private BuilderContextV() {
    throw new UnsupportedOperationException("no instances");
  }
}
