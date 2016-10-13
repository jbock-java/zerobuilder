package net.zerobuilder.compiler.generate;

import com.squareup.javapoet.CodeBlock;
import com.squareup.javapoet.FieldSpec;
import com.squareup.javapoet.MethodSpec;
import com.squareup.javapoet.ParameterSpec;
import com.squareup.javapoet.TypeName;
import net.zerobuilder.compiler.generate.DtoRegularGoal.ConstructorGoalContext;
import net.zerobuilder.compiler.generate.DtoRegularGoal.MethodGoalContext;
import net.zerobuilder.compiler.generate.DtoRegularGoal.RegularGoalContext;
import net.zerobuilder.compiler.generate.DtoRegularGoal.RegularGoalContextCases;
import net.zerobuilder.compiler.generate.DtoStep.CollectionInfo;
import net.zerobuilder.compiler.generate.DtoStep.RegularStep;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static com.squareup.javapoet.MethodSpec.methodBuilder;
import static com.squareup.javapoet.TypeName.VOID;
import static javax.lang.model.element.Modifier.PUBLIC;
import static net.zerobuilder.compiler.generate.DtoGoal.GoalMethodType.INSTANCE_METHOD;
import static net.zerobuilder.compiler.generate.DtoRegularGoal.fields;
import static net.zerobuilder.compiler.generate.DtoRegularGoal.regularGoalContextCases;
import static net.zerobuilder.compiler.generate.DtoRegularGoal.regularSteps;
import static net.zerobuilder.compiler.generate.Step.nullCheck;
import static net.zerobuilder.compiler.generate.Utilities.emptyCodeBlock;
import static net.zerobuilder.compiler.generate.Utilities.parameterSpec;
import static net.zerobuilder.compiler.generate.Utilities.presentInstances;
import static net.zerobuilder.compiler.generate.Utilities.statement;

final class BuilderV {

  static final Function<RegularGoalContext, List<FieldSpec>> fieldsV
      = goal -> {
    List<RegularStep> steps = regularSteps.apply(goal);
    return Stream.concat(
        presentInstances(fields.apply(goal)).stream(),
        steps.stream()
            .limit(steps.size() - 1)
            .map(RegularStep::field))
        .collect(Collectors.toList());
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
    Optional<CollectionInfo> maybeEmptyOption = step.collectionInfo();
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

  static final Function<RegularGoalContext, CodeBlock> regularInvoke =
      regularGoalContextCases(
          goal -> statement("return new $T($L)", goal.type(),
              invocationParameters(goal.parameterNames())),
          goal ->
              methodGoalInvocation(goal,
                  invocationParameters(goal.parameterNames())));

  private static RegularGoalContextCases<CodeBlock> emptyCollectionInvoke(final RegularStep step,
                                                                          final CollectionInfo collectionInfo) {
    return new RegularGoalContextCases<CodeBlock>() {
      @Override
      public CodeBlock constructorGoal(ConstructorGoalContext goal) {
        CodeBlock parameters = invocationParameters(goal.parameterNames());
        TypeName type = step.validParameter.type;
        String name = step.validParameter.name;
        return CodeBlock.builder()
            .addStatement("$T $N = $L", type, name, collectionInfo.initializer)
            .addStatement("return new $T($L)", goal.type(), parameters)
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
    TypeName type = goal.type();
    String method = goal.goal.details.methodName;
    builder.add(goal.goal.details.methodType == INSTANCE_METHOD ?
        statement("return this.$N.$N($L)", goal.field(), method, parameters) :
        CodeBlock.builder()
            .add(VOID.equals(type) ? emptyCodeBlock : CodeBlock.of("return "))
            .addStatement("$T.$N($L)", goal.context.type, method, parameters)
            .build());
    return builder.build();
  }

  private static CodeBlock invocationParameters(List<String> parameterNames) {
    return CodeBlock.of(String.join(", ", parameterNames));
  }

  private BuilderV() {
    throw new UnsupportedOperationException("no instances");
  }
}
