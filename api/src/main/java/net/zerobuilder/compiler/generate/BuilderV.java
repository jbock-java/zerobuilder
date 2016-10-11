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
import net.zerobuilder.compiler.generate.DtoStep.CollectionInfo;
import net.zerobuilder.compiler.generate.DtoStep.RegularStep;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.function.Function;
import java.util.stream.Collectors;

import static com.squareup.javapoet.MethodSpec.methodBuilder;
import static com.squareup.javapoet.TypeName.VOID;
import static java.util.Collections.emptyList;
import static java.util.Collections.singletonList;
import static javax.lang.model.element.Modifier.PUBLIC;
import static net.zerobuilder.compiler.generate.DtoGoal.GoalMethodType.INSTANCE_METHOD;
import static net.zerobuilder.compiler.generate.DtoRegularGoalContext.asFunction;
import static net.zerobuilder.compiler.generate.DtoRegularGoalContext.isInstance;
import static net.zerobuilder.compiler.generate.DtoRegularGoalContext.regularSteps;
import static net.zerobuilder.compiler.generate.Step.nullCheck;
import static net.zerobuilder.compiler.generate.Utilities.parameterSpec;
import static net.zerobuilder.compiler.generate.Utilities.presentInstances;
import static net.zerobuilder.compiler.generate.Utilities.statement;

final class BuilderV {

  static final Function<RegularGoalContext, List<FieldSpec>> fieldsV
      = goal -> {
    List<FieldSpec> builder = new ArrayList<>();
    DtoBuildersContext.BuildersContext context = DtoRegularGoalContext.buildersContext.apply(goal);
    builder.addAll(isInstance.test(goal)
        ? singletonList(context.field())
        : emptyList());
    regularSteps.apply(goal).stream()
        .limit(regularSteps.apply(goal).size() - 1)
        .map(RegularStep::field)
        .forEach(builder::add);
    return builder;
  };

  static final Function<RegularGoalContext, List<MethodSpec>> stepsV
      = goal -> {
    List<RegularStep> steps = regularSteps.apply(goal);
    List<MethodSpec> builder = new ArrayList<>();
    for (RegularStep step : steps.subList(0, steps.size() - 1)) {
      builder.addAll(regularMethods(step, goal, false));
    }
    builder.addAll(regularMethods(steps.get(steps.size() - 1), goal, true));
    return builder;
  };

  private static List<MethodSpec> regularMethods(
      RegularStep step, RegularGoalContext goal, boolean isLast) {
    List<MethodSpec> builder = new ArrayList<>();
    builder.add(stepMethod(step, goal, isLast));
    builder.addAll(presentInstances(maybeEmptyCollection(step, goal, isLast)));
    return builder;
  }

  private static MethodSpec stepMethod(RegularStep step, RegularGoalContext goal, boolean isLast) {
    TypeName type = step.validParameter.type;
    String name = step.validParameter.name;
    ParameterSpec parameter = parameterSpec(type, name);
    return methodBuilder(step.validParameter.name)
        .addAnnotation(Override.class)
        .addParameter(parameter)
        .returns(step.nextType)
        .addCode(nullCheck.apply(step))
        .addCode(normalAssignment(step, goal, isLast))
        .addModifiers(PUBLIC)
        .addExceptions(step.declaredExceptions)
        .build();
  }

  private static Optional<MethodSpec> maybeEmptyCollection(
      RegularStep step, RegularGoalContext goal, boolean isLast) {
    Optional<CollectionInfo> maybeEmptyOption = step.emptyOption();
    if (!maybeEmptyOption.isPresent()) {
      return Optional.empty();
    }
    CollectionInfo collectionInfo = maybeEmptyOption.get();
    return Optional.of(methodBuilder(collectionInfo.name)
        .addAnnotation(Override.class)
        .returns(step.nextType)
        .addCode(emptyCollectionAssignment(step, goal, collectionInfo, isLast))
        .addModifiers(PUBLIC)
        .build());
  }

  private static CodeBlock normalAssignment(RegularStep step, RegularGoalContext goal, boolean isLast) {
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

  private static CodeBlock emptyCollectionAssignment(RegularStep step, RegularGoalContext goal,
                                                     CollectionInfo collInfo, boolean isLast) {
    if (isLast) {
      return goal.acceptRegular(emptyCollectionInvoke(step, collInfo));
    } else {
      return CodeBlock.builder()
          .addStatement("this.$N = $L", step.field(), collInfo.initializer)
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
                                                                          final CollectionInfo collectionInfo) {
    return new RegularGoalContextCases<CodeBlock>() {
      @Override
      public CodeBlock constructorGoal(ConstructorGoalContext goal) {
        CodeBlock parameters = invocationParameters(goal.goal.details.parameterNames);
        TypeName type = step.validParameter.type;
        String name = step.validParameter.name;
        return CodeBlock.builder()
            .addStatement("$T $N = $L", type, name, collectionInfo.initializer)
            .addStatement("return new $T($L)", goal.goal.details.goalType, parameters)
            .build();
      }
      @Override
      public CodeBlock methodGoal(MethodGoalContext goal) {
        CodeBlock parameters = invocationParameters(goal.goal.details.parameterNames);
        TypeName type = step.validParameter.type;
        String name = step.validParameter.name;
        return CodeBlock.builder()
            .addStatement("$T $N = $L", type, name, collectionInfo.initializer)
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
        ? statement("$N.$N($L)", goal.builders.field(), method, parameters)
        : statement("$T.$N($L)", goal.builders.type, method, parameters));
    return builder.build();
  }

  private static CodeBlock invocationParameters(List<String> parameterNames) {
    return CodeBlock.of(String.join(", ", parameterNames));
  }

  private BuilderV() {
    throw new UnsupportedOperationException("no instances");
  }
}
