package net.zerobuilder.compiler.generate;

import com.google.common.base.Function;
import com.google.common.base.Joiner;
import com.google.common.collect.ImmutableList;
import com.squareup.javapoet.CodeBlock;
import com.squareup.javapoet.FieldSpec;
import com.squareup.javapoet.MethodSpec;
import com.squareup.javapoet.TypeName;
import net.zerobuilder.compiler.generate.DtoRegularGoalContext.ConstructorGoalContext;
import net.zerobuilder.compiler.generate.DtoRegularGoalContext.MethodGoalContext;
import net.zerobuilder.compiler.generate.DtoRegularGoalContext.RegularGoalContext;
import net.zerobuilder.compiler.generate.DtoRegularGoalContext.RegularGoalContextCases;
import net.zerobuilder.compiler.generate.DtoStep.RegularStep;

import static com.google.common.base.Optional.presentInstances;
import static com.google.common.collect.ImmutableList.of;
import static com.google.common.collect.Iterables.getLast;
import static com.squareup.javapoet.MethodSpec.methodBuilder;
import static com.squareup.javapoet.TypeName.VOID;
import static javax.lang.model.element.Modifier.PUBLIC;
import static net.zerobuilder.compiler.Utilities.statement;
import static net.zerobuilder.compiler.generate.StepContext.nullCheck;
import static net.zerobuilder.compiler.generate.UpdaterContextV.instanceField;

final class BuilderContextV {

  static final Function<RegularGoalContext, ImmutableList<FieldSpec>> fields
      = new Function<RegularGoalContext, ImmutableList<FieldSpec>>() {
    @Override
    public ImmutableList<FieldSpec> apply(RegularGoalContext goal) {
      ImmutableList.Builder<FieldSpec> builder = ImmutableList.builder();
      builder.addAll(presentInstances(of(goal.acceptRegular(instanceField))));
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
            .addStatement("this.$N = $N", step.field, step.parameter())
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
        .addParameter(step.parameter())
        .addExceptions(thrownTypes)
        .addModifiers(PUBLIC)
        .returns(step.nextType)
        .addCode(step.accept(nullCheck))
        .addCode(finalBlock).build();
  }

  static final Function<RegularGoalContext, CodeBlock> invoke
      = new Function<RegularGoalContext, CodeBlock>() {
    @Override
    public CodeBlock apply(RegularGoalContext goal) {
      return goal.acceptRegular(invokeCases);
    }
  };

  private static final RegularGoalContextCases<CodeBlock> invokeCases
      = new RegularGoalContextCases<CodeBlock>() {
    @Override
    public CodeBlock constructorGoal(ConstructorGoalContext goal) {
      CodeBlock parameters = CodeBlock.of(Joiner.on(", ").join(goal.goal.parameterNames));
      return statement("return new $T($L)", goal.goal.goalType, parameters);
    }
    @Override
    public CodeBlock methodGoal(MethodGoalContext goal) {
      CodeBlock parameters = CodeBlock.of(Joiner.on(", ").join(goal.goal.parameterNames));
      CodeBlock.Builder builder = CodeBlock.builder();
      builder.add(CodeBlock.of(VOID.equals(goal.goal.goalType) ? "" : "return "));
      String method = goal.goal.methodName;
      builder.add(goal.goal.instance
          ? statement("$N.$N($L)", goal.builders.field, method, parameters)
          : statement("$T.$N($L)", goal.builders.type, method, parameters));
      return builder.build();
    }
  };

  private BuilderContextV() {
    throw new UnsupportedOperationException("no instances");
  }
}
