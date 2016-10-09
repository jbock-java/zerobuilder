package net.zerobuilder.compiler.generate;

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

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.function.Function;

import static com.squareup.javapoet.MethodSpec.methodBuilder;
import static com.squareup.javapoet.TypeName.VOID;
import static javax.lang.model.element.Modifier.PUBLIC;
import static net.zerobuilder.compiler.generate.DtoGoal.GoalMethodType.INSTANCE_METHOD;
import static net.zerobuilder.compiler.generate.DtoRegularGoalContext.asFunction;
import static net.zerobuilder.compiler.generate.DtoRegularGoalContext.isInstance;
import static net.zerobuilder.compiler.generate.DtoRegularGoalContext.regularSteps;
import static net.zerobuilder.compiler.generate.DtoStep.declaredExceptions;
import static net.zerobuilder.compiler.generate.StepContext.nullCheck;
import static net.zerobuilder.compiler.generate.Utilities.parameterSpec;
import static net.zerobuilder.compiler.generate.Utilities.presentInstances;
import static net.zerobuilder.compiler.generate.Utilities.statement;

final class BuilderContextV {

  static final Function<RegularGoalContext, List<FieldSpec>> fields
      = goal -> {
    List<FieldSpec> builder = new ArrayList<>();
    DtoBuildersContext.BuildersContext buildersContext = DtoRegularGoalContext.buildersContext.apply(goal);
    builder.addAll(isInstance.apply(goal)
        ? Collections.singletonList(buildersContext.field)
        : Collections.emptyList());
    List<RegularStep> steps1 = regularSteps.apply(goal);
    for (RegularStep step : steps1.subList(0, steps1.size() - 1)) {
      builder.add(step.field());
    }
    return builder;
  };

  static final Function<RegularGoalContext, List<MethodSpec>> steps
      = goal -> {
    List<RegularStep> steps1 = regularSteps.apply(goal);
    List<MethodSpec> builder = new ArrayList<>();
    for (RegularStep step : steps1.subList(0, steps1.size() - 1)) {
      builder.addAll(regularMethods(step, goal, false));
    }
    builder.addAll(regularMethods(steps1.get(steps1.size() - 1), goal, true));
    return builder;
  };

  private static List<MethodSpec> regularMethods(RegularStep step, RegularGoalContext goal, boolean isLast) {
    List<MethodSpec> builder = new ArrayList<>();
    builder.add(regularStep(step, goal, isLast));
    builder.addAll(presentInstances(regularEmptyCollection(step, goal, isLast)));
    return builder;
  }

  private static Optional<MethodSpec> regularEmptyCollection(RegularStep step, RegularGoalContext goal, boolean isLast) {
    Optional<EmptyOption> maybeEmptyOption = step.emptyOption();
    if (!maybeEmptyOption.isPresent()) {
      return Optional.empty();
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
      CodeBlock parameters = invocationParameters(goal.goal.details.parameterNames);
      return statement("return new $T($L)", goal.goal.details.goalType, parameters);
    }
    @Override
    public CodeBlock methodGoal(MethodGoalContext goal) {
      CodeBlock parameters = invocationParameters(goal.goal.details.parameterNames);
      return methodGoalInvocation(goal, parameters);
    }
  });

  private static RegularGoalContextCases<CodeBlock> emptyCollectionInvoke(final RegularStep step,
                                                                          final EmptyOption emptyOption) {
    return new RegularGoalContextCases<CodeBlock>() {
      @Override
      public CodeBlock constructorGoal(ConstructorGoalContext goal) {
        CodeBlock parameters = invocationParameters(goal.goal.details.parameterNames);
        TypeName type = step.validParameter.type;
        String name = step.validParameter.name;
        return CodeBlock.builder()
            .addStatement("$T $N = $L", type, name, emptyOption.initializer)
            .addStatement("return new $T($L)", goal.goal.details.goalType, parameters)
            .build();
      }
      @Override
      public CodeBlock methodGoal(MethodGoalContext goal) {
        CodeBlock parameters = invocationParameters(goal.goal.details.parameterNames);
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
    TypeName type = goal.goal.details.goalType;
    String method = goal.goal.details.methodName;
    builder.add(CodeBlock.of(VOID.equals(type) ? "" : "return "));
    builder.add(goal.goal.details.methodType == INSTANCE_METHOD
        ? statement("$N.$N($L)", goal.builders.field, method, parameters)
        : statement("$T.$N($L)", goal.builders.type, method, parameters));
    return builder.build();
  }

  private static CodeBlock invocationParameters(List<String> parameterNames) {
    return CodeBlock.of(String.join(", ", parameterNames));
  }

  private BuilderContextV() {
    throw new UnsupportedOperationException("no instances");
  }
}
