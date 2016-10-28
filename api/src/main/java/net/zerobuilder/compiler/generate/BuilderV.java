package net.zerobuilder.compiler.generate;

import com.squareup.javapoet.CodeBlock;
import com.squareup.javapoet.FieldSpec;
import com.squareup.javapoet.MethodSpec;
import com.squareup.javapoet.ParameterSpec;
import com.squareup.javapoet.TypeName;
import net.zerobuilder.compiler.generate.DtoConstructorGoal.SimpleConstructorGoalContext;
import net.zerobuilder.compiler.generate.DtoMethodGoal.SimpleMethodGoalContext;
import net.zerobuilder.compiler.generate.DtoRegularGoal.SimpleRegularGoalContext;
import net.zerobuilder.compiler.generate.DtoRegularGoal.RegularGoalContextCases;
import net.zerobuilder.compiler.generate.DtoRegularStep.AbstractRegularStep;
import net.zerobuilder.compiler.generate.DtoStep.CollectionInfo;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static com.squareup.javapoet.MethodSpec.methodBuilder;
import static javax.lang.model.element.Modifier.PUBLIC;
import static net.zerobuilder.compiler.generate.DtoRegularGoal.regularGoalContextCases;
import static net.zerobuilder.compiler.generate.DtoStep.AbstractStep.nextType;
import static net.zerobuilder.compiler.generate.Step.nullCheck;
import static net.zerobuilder.compiler.generate.Utilities.parameterSpec;
import static net.zerobuilder.compiler.generate.Utilities.presentInstances;
import static net.zerobuilder.compiler.generate.Utilities.statement;

final class BuilderV {

  private final Builder builder;

  BuilderV(Builder builder) {
    this.builder = builder;
  }

  final Function<SimpleRegularGoalContext, List<FieldSpec>> fieldsV
      = goal -> {
    List<? extends AbstractRegularStep> steps = goal.regularSteps();
    return Stream.concat(
        presentInstances(goal.fields()).stream(),
        steps.stream()
            .limit(steps.size() - 1)
            .map(AbstractRegularStep::field))
        .collect(Collectors.toList());
  };

  final Function<SimpleRegularGoalContext, List<MethodSpec>> stepsV
      = goal -> {
    List<? extends AbstractRegularStep> steps = goal.regularSteps();
    List<MethodSpec> builder = new ArrayList<>();
    for (AbstractRegularStep step : steps.subList(0, steps.size() - 1)) {
      builder.addAll(regularMethods(step, goal, false));
    }
    builder.addAll(regularMethods(steps.get(steps.size() - 1), goal, true));
    return builder;
  };

  private List<MethodSpec> regularMethods(
      AbstractRegularStep step, SimpleRegularGoalContext goal, boolean isLast) {
    List<MethodSpec> builder = new ArrayList<>();
    builder.add(stepMethod(step, goal, isLast));
    builder.addAll(presentInstances(maybeEmptyCollection(step, goal, isLast)));
    return builder;
  }

  private MethodSpec stepMethod(AbstractRegularStep step, SimpleRegularGoalContext goal, boolean isLast) {
    TypeName type = step.regularParameter().type;
    String name = step.regularParameter().name;
    ParameterSpec parameter = parameterSpec(type, name);
    return methodBuilder(step.regularParameter().name)
        .addAnnotation(Override.class)
        .addParameter(parameter)
        .returns(nextType(step))
        .addCode(nullCheck.apply(step))
        .addCode(normalAssignment(step, goal, isLast))
        .addModifiers(PUBLIC)
        .addExceptions(step.declaredExceptions())
        .build();
  }

  private Optional<MethodSpec> maybeEmptyCollection(
      AbstractRegularStep step, SimpleRegularGoalContext goal, boolean isLast) {
    Optional<CollectionInfo> maybeEmptyOption = step.collectionInfo();
    if (!maybeEmptyOption.isPresent()) {
      return Optional.empty();
    }
    CollectionInfo collectionInfo = maybeEmptyOption.get();
    return Optional.of(methodBuilder(collectionInfo.name)
        .addAnnotation(Override.class)
        .returns(nextType(step))
        .addCode(emptyCollectionAssignment(step, goal, collectionInfo, isLast))
        .addModifiers(PUBLIC)
        .build());
  }

  private CodeBlock normalAssignment(AbstractRegularStep step, SimpleRegularGoalContext goal, boolean isLast) {
    TypeName type = step.regularParameter().type;
    String name = step.regularParameter().name;
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

  private CodeBlock emptyCollectionAssignment(AbstractRegularStep step, SimpleRegularGoalContext goal,
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

  private final Function<SimpleRegularGoalContext, CodeBlock> regularInvoke =
      regularGoalContextCases(
          goal -> statement("return new $T($L)", goal.type(),
              goal.invocationParameters()),
          goal -> goal.methodGoalInvocation());

  final RegularGoalContextCases<CodeBlock> emptyCollectionInvoke(final AbstractRegularStep step,
                                                                 final CollectionInfo collectionInfo) {
    return new RegularGoalContextCases<CodeBlock>() {
      @Override
      public CodeBlock constructorGoal(SimpleConstructorGoalContext goal) {
        CodeBlock parameters = goal.invocationParameters();
        TypeName type = step.regularParameter().type;
        String name = step.regularParameter().name;
        return CodeBlock.builder()
            .addStatement("$T $N = $L", type, name, collectionInfo.initializer)
            .addStatement("return new $T($L)", goal.type(), parameters)
            .build();
      }
      @Override
      public CodeBlock methodGoal(SimpleMethodGoalContext goal) {
        TypeName type = step.regularParameter().type;
        String name = step.regularParameter().name;
        return CodeBlock.builder()
            .addStatement("$T $N = $L", type, name, collectionInfo.initializer)
            .add(goal.methodGoalInvocation())
            .build();
      }
    };
  }
}
