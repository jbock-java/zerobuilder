package net.zerobuilder.compiler.generate;

import com.google.common.base.Function;
import com.google.common.base.Joiner;
import com.google.common.base.Optional;
import com.google.common.collect.ImmutableList;
import com.squareup.javapoet.CodeBlock;
import com.squareup.javapoet.FieldSpec;
import com.squareup.javapoet.MethodSpec;
import com.squareup.javapoet.ParameterSpec;
import com.squareup.javapoet.TypeName;
import net.zerobuilder.compiler.generate.DtoRegularGoalContext.ConstructorGoalContext;
import net.zerobuilder.compiler.generate.DtoRegularGoalContext.MethodGoalContext;
import net.zerobuilder.compiler.generate.DtoRegularGoalContext.RegularGoalContext;
import net.zerobuilder.compiler.generate.DtoRegularGoalContext.RegularGoalContextCases;
import net.zerobuilder.compiler.generate.DtoStep.EmptyOption;
import net.zerobuilder.compiler.generate.DtoStep.RegularStep;

import static com.google.common.base.Optional.absent;
import static com.google.common.base.Optional.presentInstances;
import static com.google.common.collect.ImmutableList.of;
import static com.google.common.collect.Iterables.getLast;
import static com.squareup.javapoet.MethodSpec.methodBuilder;
import static com.squareup.javapoet.TypeName.VOID;
import static javax.lang.model.element.Modifier.PUBLIC;
import static net.zerobuilder.compiler.Utilities.parameterSpec;
import static net.zerobuilder.compiler.Utilities.statement;
import static net.zerobuilder.compiler.generate.DtoRegularGoalContext.asFunction;
import static net.zerobuilder.compiler.generate.DtoRegularGoalContext.isInstance;
import static net.zerobuilder.compiler.generate.DtoStep.declaredExceptions;
import static net.zerobuilder.compiler.generate.StepContext.nullCheck;

final class BuilderContextV {

  static final Function<RegularGoalContext, ImmutableList<FieldSpec>> fields
      = new Function<RegularGoalContext, ImmutableList<FieldSpec>>() {
    @Override
    public ImmutableList<FieldSpec> apply(RegularGoalContext goal) {
      ImmutableList.Builder<FieldSpec> builder = ImmutableList.builder();
      builder.addAll(isInstance.apply(goal)
          ? ImmutableList.of(goal.builders.field)
          : ImmutableList.<FieldSpec>of());
      for (RegularStep step : goal.steps.subList(0, goal.steps.size() - 1)) {
        builder.add(step.field());
      }
      return builder.build();
    }
  };

  static final Function<RegularGoalContext, ImmutableList<MethodSpec>> steps
      = new Function<RegularGoalContext, ImmutableList<MethodSpec>>() {
    @Override
    public ImmutableList<MethodSpec> apply(RegularGoalContext goal) {
      ImmutableList.Builder<MethodSpec> builder = ImmutableList.builder();
      for (RegularStep step : goal.steps.subList(0, goal.steps.size() - 1)) {
        builder.addAll(regularMethods(step, goal, false));
      }
      builder.addAll(regularMethods(getLast(goal.steps), goal, true));
      return builder.build();
    }
  };

  private static ImmutableList<MethodSpec> regularMethods(RegularStep step, RegularGoalContext goal, boolean isLast) {
    ImmutableList.Builder<MethodSpec> builder = ImmutableList.builder();
    builder.add(regularStep(step, goal, isLast));
    builder.addAll(presentInstances(of(regularEmptyCollection(step, goal, isLast))));
    return builder.build();
  }

  private static Optional<MethodSpec> regularEmptyCollection(RegularStep step, RegularGoalContext goal, boolean isLast) {
    Optional<EmptyOption> maybeEmptyOption = step.emptyOption();
    if (!maybeEmptyOption.isPresent()) {
      return absent();
    }
    EmptyOption emptyOption = maybeEmptyOption.get();
    return Optional.of(methodBuilder(emptyOption.name)
        .addAnnotation(Override.class)
        .returns(step.nextType)
        .addCode(regularEmptyCollectionFinalBlock(step, goal, emptyOption, isLast))
        .addModifiers(PUBLIC)
        .build());
  }


  private static MethodSpec regularStep(RegularStep step, RegularGoalContext goal, boolean isLast) {
    TypeName type = step.validParameter.type;
    String name = step.validParameter.name;
    ParameterSpec parameter = parameterSpec(type, name);
    return methodBuilder(step.validParameter.name)
        .addAnnotation(Override.class)
        .addParameter(parameter)
        .returns(step.nextType)
        .addCode(nullCheck.apply(step))
        .addCode(regularFinalBlock(step, goal, isLast))
        .addModifiers(PUBLIC)
        .addExceptions(declaredExceptions.apply(step))
        .build();
  }

  private static CodeBlock regularFinalBlock(RegularStep step, RegularGoalContext goal, boolean isLast) {
    TypeName type = step.validParameter.type;
    String name = step.validParameter.name;
    ParameterSpec parameter = parameterSpec(type, name);
    if (isLast) {
      return regularInvoke.apply(goal);
    } else {
      return CodeBlock.builder()
          .addStatement("this.$N = $N", step.field(), parameter)
          .addStatement("return this")
          .build();
    }
  }

  private static CodeBlock regularEmptyCollectionFinalBlock(RegularStep step, RegularGoalContext goal,
                                                            EmptyOption emptyOption, boolean isLast) {
    if (isLast) {
      return goal.acceptRegular(emptyCollectionInvoke(step, emptyOption));
    } else {
      return CodeBlock.builder()
          .addStatement("this.$N = $L", step.field(), emptyOption.initializer)
          .addStatement("return this")
          .build();
    }
  }

  static final Function<RegularGoalContext, CodeBlock> regularInvoke
      = asFunction(new RegularGoalContextCases<CodeBlock>() {
    @Override
    public CodeBlock constructorGoal(ConstructorGoalContext goal) {
      CodeBlock parameters = invocationParameters(goal.goal.parameterNames);
      return statement("return new $T($L)", goal.goal.goalType, parameters);
    }
    @Override
    public CodeBlock methodGoal(MethodGoalContext goal) {
      CodeBlock parameters = invocationParameters(goal.goal.parameterNames);
      return methodGoalInvocation(goal, parameters);
    }
  });

  private static RegularGoalContextCases<CodeBlock> emptyCollectionInvoke(final RegularStep step,
                                                                          final EmptyOption emptyOption) {
    return new RegularGoalContextCases<CodeBlock>() {
      @Override
      public CodeBlock constructorGoal(ConstructorGoalContext goal) {
        CodeBlock parameters = invocationParameters(goal.goal.parameterNames);
        TypeName type = step.validParameter.type;
        String name = step.validParameter.name;
        return CodeBlock.builder()
            .addStatement("$T $N = $L", type, name, emptyOption.initializer)
            .addStatement("return new $T($L)", goal.goal.goalType, parameters)
            .build();
      }
      @Override
      public CodeBlock methodGoal(MethodGoalContext goal) {
        CodeBlock parameters = invocationParameters(goal.goal.parameterNames);
        TypeName type = step.validParameter.type;
        String name = step.validParameter.name;
        return CodeBlock.builder()
            .addStatement("$T $N = $L", type, name, emptyOption.initializer)
            .add(methodGoalInvocation(goal, parameters))
            .build();
      }
    };
  }

  private static CodeBlock methodGoalInvocation(MethodGoalContext goal, CodeBlock parameters) {
    CodeBlock.Builder builder = CodeBlock.builder();
    TypeName type = goal.goal.goalType;
    String method = goal.goal.methodName;
    builder.add(CodeBlock.of(VOID.equals(type) ? "" : "return "));
    builder.add(goal.goal.instance
        ? statement("$N.$N($L)", goal.builders.field, method, parameters)
        : statement("$T.$N($L)", goal.builders.type, method, parameters));
    return builder.build();
  }

  private static CodeBlock invocationParameters(ImmutableList<String> parameterNames) {
    return CodeBlock.of(Joiner.on(", ").join(parameterNames));
  }

  private BuilderContextV() {
    throw new UnsupportedOperationException("no instances");
  }
}
