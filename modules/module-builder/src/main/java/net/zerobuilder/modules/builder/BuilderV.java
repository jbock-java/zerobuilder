package net.zerobuilder.modules.builder;

import com.squareup.javapoet.CodeBlock;
import com.squareup.javapoet.FieldSpec;
import com.squareup.javapoet.MethodSpec;
import com.squareup.javapoet.ParameterSpec;
import com.squareup.javapoet.TypeName;
import net.zerobuilder.compiler.generate.DtoConstructorGoal;
import net.zerobuilder.compiler.generate.DtoConstructorGoal.SimpleConstructorGoalContext;
import net.zerobuilder.compiler.generate.DtoMethodGoal.InstanceMethodGoalContext;
import net.zerobuilder.compiler.generate.DtoMethodGoal.SimpleStaticMethodGoalContext;
import net.zerobuilder.compiler.generate.DtoRegularGoal.RegularGoalContextCases;
import net.zerobuilder.compiler.generate.DtoRegularGoal.SimpleRegularGoalContext;
import net.zerobuilder.compiler.generate.DtoRegularStep.AbstractRegularStep;
import net.zerobuilder.compiler.generate.DtoStep.CollectionInfo;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.function.Function;
import java.util.stream.Stream;

import static com.squareup.javapoet.MethodSpec.methodBuilder;
import static com.squareup.javapoet.TypeName.VOID;
import static java.util.stream.Collectors.toList;
import static javax.lang.model.element.Modifier.PUBLIC;
import static net.zerobuilder.compiler.generate.DtoRegularGoal.regularGoalContextCases;
import static net.zerobuilder.compiler.generate.DtoStep.AbstractStep.nextType;
import static net.zerobuilder.compiler.generate.Step.nullCheck;
import static net.zerobuilder.compiler.generate.ZeroUtil.concat;
import static net.zerobuilder.compiler.generate.ZeroUtil.emptyCodeBlock;
import static net.zerobuilder.compiler.generate.ZeroUtil.flatList;
import static net.zerobuilder.compiler.generate.ZeroUtil.parameterSpec;
import static net.zerobuilder.compiler.generate.ZeroUtil.presentInstances;
import static net.zerobuilder.compiler.generate.ZeroUtil.statement;

final class BuilderV {

  private final Builder builder;

  BuilderV(Builder builder) {
    this.builder = builder;
  }

  final Function<SimpleRegularGoalContext, List<FieldSpec>> fieldsV
      = goal -> {
    List<? extends AbstractRegularStep> steps = goal.regularSteps();
    return Stream.concat(
        presentInstances(goal.maybeField()).stream(),
        steps.stream()
            .limit(steps.size() - 1)
            .map(AbstractRegularStep::field))
        .collect(toList());
  };

  final Function<SimpleRegularGoalContext, List<MethodSpec>> stepsV =
      goal -> goal.regularSteps().stream()
          .map(step -> regularMethods(step, goal))
          .collect(flatList());

  private List<MethodSpec> regularMethods(
      AbstractRegularStep step, SimpleRegularGoalContext goal) {
    List<MethodSpec> builder = new ArrayList<>();
    builder.add(stepMethod(step, goal));
    builder.addAll(presentInstances(maybeEmptyCollection(step, goal)));
    return builder;
  }

  private MethodSpec stepMethod(AbstractRegularStep step, SimpleRegularGoalContext goal) {
    TypeName type = step.regularParameter().type;
    String name = step.regularParameter().name;
    ParameterSpec parameter = parameterSpec(type, name);
    List<TypeName> thrownTypes = step.declaredExceptions();
    if (step.isLast()) {
      thrownTypes = concat(thrownTypes, goal.thrownTypes);
    }
    TypeName nextType = nextType(step);
    return methodBuilder(step.regularParameter().name)
        .addAnnotation(Override.class)
        .addParameter(parameter)
        .returns(nextType)
        .addCode(nullCheck.apply(step))
        .addCode(normalAssignment(step, goal))
        .addModifiers(PUBLIC)
        .addExceptions(thrownTypes)
        .build();
  }

  private Optional<MethodSpec> maybeEmptyCollection(
      AbstractRegularStep step, SimpleRegularGoalContext goal) {
    Optional<CollectionInfo> maybeEmptyOption = step.collectionInfo();
    if (!maybeEmptyOption.isPresent()) {
      return Optional.empty();
    }
    CollectionInfo collectionInfo = maybeEmptyOption.get();
    return Optional.of(methodBuilder(collectionInfo.name)
        .addAnnotation(Override.class)
        .returns(nextType(step))
        .addCode(emptyCollectionAssignment(step, goal, collectionInfo))
        .addModifiers(PUBLIC)
        .build());
  }

  private CodeBlock normalAssignment(AbstractRegularStep step, SimpleRegularGoalContext goal) {
    TypeName type = step.regularParameter().type;
    String name = step.regularParameter().name;
    ParameterSpec parameter = parameterSpec(type, name);
    if (step.isLast()) {
      return regularInvoke.apply(goal);
    } else {
      return CodeBlock.builder()
          .addStatement("this.$N = $N", step.field(), parameter)
          .addStatement("return this")
          .build();
    }
  }

  private CodeBlock emptyCollectionAssignment(AbstractRegularStep step, SimpleRegularGoalContext goal,
                                              CollectionInfo collInfo) {
    return step.isLast() ?
        goal.acceptRegular(emptyCollectionInvoke(step, collInfo)) :
        CodeBlock.builder()
            .addStatement("this.$N = $L", step.field(), collInfo.initializer)
            .addStatement("return this")
            .build();
  }

  private final Function<SimpleRegularGoalContext, CodeBlock> regularInvoke =
      regularGoalContextCases(
          this::constructorCall,
          this::instanceCall,
          this::staticCall);

  CodeBlock constructorCall(SimpleConstructorGoalContext constructor) {
    return statement("return new $T($L)", constructor.type(),
        constructor.invocationParameters());
  }

  CodeBlock instanceCall(InstanceMethodGoalContext goal) {
    TypeName type = goal.type();
    String method = goal.details.methodName;
    return CodeBlock.builder()
        .add(VOID.equals(type) ? emptyCodeBlock : CodeBlock.of("return "))
        .addStatement("this.$N.$N($L)", goal.field(), method, goal.invocationParameters())
        .build();
  }

  CodeBlock staticCall(SimpleStaticMethodGoalContext goal) {
    TypeName type = goal.type();
    String method = goal.details.methodName;
    return CodeBlock.builder()
        .add(VOID.equals(type) ? emptyCodeBlock : CodeBlock.of("return "))
        .addStatement("$T.$N($L)", goal.context.type, method, goal.invocationParameters())
        .build();
  }

  RegularGoalContextCases<CodeBlock> emptyCollectionInvoke(AbstractRegularStep step,
                                                           CollectionInfo collectionInfo) {
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
      public CodeBlock methodGoal(InstanceMethodGoalContext goal) {
        TypeName type = step.regularParameter().type;
        String name = step.regularParameter().name;
        return CodeBlock.builder()
            .addStatement("$T $N = $L", type, name, collectionInfo.initializer)
            .add(instanceCall(goal))
            .build();
      }
      @Override
      public CodeBlock staticMethodGoal(SimpleStaticMethodGoalContext goal) {
        TypeName type = step.regularParameter().type;
        String name = step.regularParameter().name;
        return CodeBlock.builder()
            .addStatement("$T $N = $L", type, name, collectionInfo.initializer)
            .add(staticCall(goal))
            .build();
      }
    };
  }
}
