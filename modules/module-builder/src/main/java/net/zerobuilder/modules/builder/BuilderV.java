package net.zerobuilder.modules.builder;

import com.squareup.javapoet.CodeBlock;
import com.squareup.javapoet.FieldSpec;
import com.squareup.javapoet.MethodSpec;
import com.squareup.javapoet.ParameterSpec;
import com.squareup.javapoet.TypeName;
import net.zerobuilder.compiler.generate.DtoConstructorGoal.SimpleConstructorGoalContext;
import net.zerobuilder.compiler.generate.DtoMethodGoal.InstanceMethodGoalContext;
import net.zerobuilder.compiler.generate.DtoMethodGoal.SimpleStaticMethodGoalContext;
import net.zerobuilder.compiler.generate.DtoRegularGoal.SimpleRegularGoalContext;
import net.zerobuilder.compiler.generate.DtoRegularStep.AbstractRegularStep;
import net.zerobuilder.compiler.generate.DtoStep;

import java.util.List;
import java.util.function.Function;
import java.util.stream.Stream;

import static com.squareup.javapoet.MethodSpec.methodBuilder;
import static com.squareup.javapoet.TypeName.BOOLEAN;
import static com.squareup.javapoet.TypeName.VOID;
import static java.util.Arrays.asList;
import static java.util.function.Function.identity;
import static java.util.stream.Collectors.toList;
import static javax.lang.model.element.Modifier.PRIVATE;
import static javax.lang.model.element.Modifier.PUBLIC;
import static net.zerobuilder.compiler.generate.DtoContext.ContextLifecycle.REUSE_INSTANCES;
import static net.zerobuilder.compiler.generate.DtoRegularGoal.regularGoalContextCases;
import static net.zerobuilder.compiler.generate.ZeroUtil.concat;
import static net.zerobuilder.compiler.generate.ZeroUtil.downcase;
import static net.zerobuilder.compiler.generate.ZeroUtil.fieldSpec;
import static net.zerobuilder.compiler.generate.ZeroUtil.joinCodeBlocks;
import static net.zerobuilder.compiler.generate.ZeroUtil.parameterSpec;
import static net.zerobuilder.compiler.generate.ZeroUtil.presentInstances;
import static net.zerobuilder.compiler.generate.ZeroUtil.rawClassName;
import static net.zerobuilder.compiler.generate.ZeroUtil.simpleName;
import static net.zerobuilder.compiler.generate.ZeroUtil.statement;
import static net.zerobuilder.compiler.generate.ZeroUtil.upcase;
import static net.zerobuilder.modules.builder.Step.nullCheck;

final class BuilderV {

  static TypeName nextType(DtoStep.AbstractStep step) {
    if (step.nextStep.isPresent()) {
      return step.context.generatedType
          .nestedClass(upcase(step.goalDetails.name() + "Builder"))
          .nestedClass(step.nextStep.get().thisType);
    }
    return step.goalDetails.type();
  }

  static final Function<SimpleRegularGoalContext, List<FieldSpec>> fieldsV
      = goal -> {
    List<? extends AbstractRegularStep> steps = goal.regularSteps();
    return asList(
        presentInstances(goal.maybeField()).stream(),
        goal.context().lifecycle == REUSE_INSTANCES ?
            Stream.of(fieldSpec(BOOLEAN, "_currently_in_use", PRIVATE)) :
            Stream.<FieldSpec>empty(),
        steps.stream()
            .limit(steps.size() - 1)
            .map(AbstractRegularStep::field))
        .stream()
        .flatMap(identity())
        .collect(toList());
  };

  static final Function<SimpleRegularGoalContext, List<MethodSpec>> stepsV =
      goal -> goal.regularSteps().stream()
          .map(step -> stepMethod(step, goal))
          .collect(toList());

  private static MethodSpec stepMethod(AbstractRegularStep step, SimpleRegularGoalContext goal) {
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

  private static CodeBlock normalAssignment(AbstractRegularStep step, SimpleRegularGoalContext goal) {
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

  private static final Function<SimpleRegularGoalContext, CodeBlock> regularInvoke =
      regularGoalContextCases(
          BuilderV::constructorCall,
          BuilderV::instanceCall,
          BuilderV::staticCall);

  private static CodeBlock constructorCall(SimpleConstructorGoalContext goal) {
    TypeName type = goal.type();
    ParameterSpec varGoal = parameterSpec(type,
        '_' + downcase(simpleName(type)));
    CodeBlock.Builder builder = CodeBlock.builder();
    if (goal.context.lifecycle == REUSE_INSTANCES) {
      builder.addStatement("this._currently_in_use = false");
    }
    return builder
        .addStatement("$T $N = new $T($L)", varGoal.type, varGoal, goal.type(), goal.invocationParameters())
        .add(free(goal.steps))
        .addStatement("return $N", varGoal)
        .build();
  }

  private static CodeBlock instanceCall(InstanceMethodGoalContext goal) {
    TypeName type = goal.type();
    String method = goal.details.methodName;
    ParameterSpec varGoal = parameterSpec(type,
        '_' + downcase(simpleName(type)));
    CodeBlock.Builder builder = CodeBlock.builder();
    if (goal.context.lifecycle == REUSE_INSTANCES) {
      builder.addStatement("this._currently_in_use = false");
    }
    if (VOID.equals(type)) {
      builder.addStatement("this.$N.$N($L)", goal.instanceField(),
          method, goal.invocationParameters());
    } else {
      builder.addStatement("$T $N = this.$N.$N($L)", varGoal.type, varGoal, goal.instanceField(),
          method, goal.invocationParameters());
    }
    if (goal.context.lifecycle == REUSE_INSTANCES) {
      builder.addStatement("this.$N = null", goal.instanceField());
    }
    builder.add(free(goal.steps));
    if (!VOID.equals(type)) {
      builder.addStatement("return $N", varGoal);
    }
    return builder.build();
  }

  private static CodeBlock staticCall(SimpleStaticMethodGoalContext goal) {
    TypeName type = goal.type();
    String method = goal.details.methodName;
    ParameterSpec varGoal = parameterSpec(type,
        '_' + downcase(simpleName(type)));
    CodeBlock.Builder builder = CodeBlock.builder();
    if (goal.context.lifecycle == REUSE_INSTANCES) {
      builder.addStatement("this._currently_in_use = false");
    }
    if (VOID.equals(type)) {
      builder.addStatement("$T.$N($L)", rawClassName(goal.context.type).get(),
          method, goal.invocationParameters());
    } else {
      builder.addStatement("$T $N = $T.$N($L)", varGoal.type, varGoal, rawClassName(goal.context.type).get(),
          method, goal.invocationParameters());
    }
    builder.add(free(goal.steps));
    if (!VOID.equals(type)) {
      builder.addStatement("return $N", varGoal);
    }
    return builder.build();
  }

  private static CodeBlock free(List<? extends AbstractRegularStep> steps) {
    return steps.stream()
        .filter(step -> !step.isLast())
        .map(step -> step.regularParameter())
        .filter(parameter -> !parameter.type.isPrimitive())
        .map(parameter -> statement("this.$N = null", parameter.name))
        .collect(joinCodeBlocks);
  }

  private BuilderV() {
    throw new UnsupportedOperationException("no instances");
  }
}
