package net.zerobuilder.modules.builder;

import com.squareup.javapoet.CodeBlock;
import com.squareup.javapoet.FieldSpec;
import com.squareup.javapoet.MethodSpec;
import com.squareup.javapoet.ParameterSpec;
import com.squareup.javapoet.TypeName;
import net.zerobuilder.compiler.generate.DtoGoalDetails;
import net.zerobuilder.compiler.generate.DtoGoalDetails.AbstractRegularDetails;
import net.zerobuilder.compiler.generate.DtoRegularGoal.SimpleRegularGoalContext;
import net.zerobuilder.compiler.generate.DtoRegularGoalDescription.SimpleRegularGoalDescription;
import net.zerobuilder.compiler.generate.DtoRegularParameter.SimpleParameter;

import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.function.Function;
import java.util.function.IntFunction;
import java.util.stream.Stream;

import static com.squareup.javapoet.MethodSpec.methodBuilder;
import static com.squareup.javapoet.TypeName.BOOLEAN;
import static com.squareup.javapoet.TypeName.VOID;
import static java.util.Collections.emptyList;
import static java.util.Collections.singletonList;
import static java.util.Optional.empty;
import static java.util.stream.Collectors.toList;
import static javax.lang.model.element.Modifier.PRIVATE;
import static javax.lang.model.element.Modifier.PUBLIC;
import static net.zerobuilder.compiler.generate.DtoContext.ContextLifecycle.REUSE_INSTANCES;
import static net.zerobuilder.compiler.generate.DtoGoalDetails.regularDetailsCases;
import static net.zerobuilder.compiler.generate.DtoRegularGoal.regularGoalContextCases;
import static net.zerobuilder.compiler.generate.ZeroUtil.downcase;
import static net.zerobuilder.compiler.generate.ZeroUtil.fieldSpec;
import static net.zerobuilder.compiler.generate.ZeroUtil.flatList;
import static net.zerobuilder.compiler.generate.ZeroUtil.joinCodeBlocks;
import static net.zerobuilder.compiler.generate.ZeroUtil.parameterSpec;
import static net.zerobuilder.compiler.generate.ZeroUtil.presentInstances;
import static net.zerobuilder.compiler.generate.ZeroUtil.rawClassName;
import static net.zerobuilder.compiler.generate.ZeroUtil.simpleName;
import static net.zerobuilder.compiler.generate.ZeroUtil.statement;
import static net.zerobuilder.compiler.generate.ZeroUtil.upcase;
import static net.zerobuilder.modules.builder.Generator.instanceField;
import static net.zerobuilder.modules.builder.Step.nullCheck;

final class Builder {

  static TypeName nextType(int i, SimpleRegularGoalContext goal) {
    if (i < goal.description.parameters.size() - 1) {
      return goal.description.context.generatedType
          .nestedClass(upcase(goal.description.details.name() + "Builder"))
          .nestedClass(upcase(goal.description.parameters.get(i + 1).name));
    }
    return goal.description.details.type();
  }

  private static final Function<SimpleRegularGoalContext, Optional<FieldSpec>> maybeField =
      regularGoalContextCases(
          constructor -> empty(),
          method -> Optional.of(instanceField(method.description)),
          staticMethod -> empty());

  static final Function<SimpleRegularGoalContext, List<FieldSpec>> fields
      = goal -> {
    List<SimpleParameter> steps = goal.description.parameters;
    return Stream.of(
        presentInstances(maybeField.apply(goal)),
        goal.description.details.lifecycle == REUSE_INSTANCES ?
            singletonList(fieldSpec(BOOLEAN, "_currently_in_use", PRIVATE)) :
            Collections.<FieldSpec>emptyList(),
        steps.stream()
            .limit(steps.size() - 1)
            .map(parameter -> fieldSpec(parameter.type, parameter.name, PRIVATE))
            .collect(toList()))
        .collect(flatList());
  };

  static IntFunction<MethodSpec> steps(SimpleRegularGoalContext goal) {
    return i -> {
      SimpleParameter step = goal.description.parameters.get(i);
      TypeName type = step.type;
      String name = step.name;
      ParameterSpec parameter = parameterSpec(type, name);
      List<TypeName> thrownTypes = i < goal.description.parameters.size() - 1 ?
          emptyList() :
          goal.description.thrownTypes;
      TypeName nextType = nextType(i, goal);
      return methodBuilder(step.name)
          .addAnnotation(Override.class)
          .addParameter(parameter)
          .returns(nextType)
          .addCode(nullCheck.apply(step))
          .addCode(normalAssignment(i, goal))
          .addModifiers(PUBLIC)
          .addExceptions(thrownTypes)
          .build();
    };
  }

  private static CodeBlock normalAssignment(int i, SimpleRegularGoalContext goal) {
    SimpleParameter step = goal.description.parameters.get(i);
    TypeName type = step.type;
    String name = step.name;
    ParameterSpec parameter = parameterSpec(type, name);
    if (i == goal.description.parameters.size() - 1) {
      return regularInvoke(goal).apply(goal.description.details);
    } else {
      return CodeBlock.builder()
          .addStatement("this.$N = $N", fieldSpec(step.type, step.name), parameter)
          .addStatement("return this")
          .build();
    }
  }

  private static final Function<AbstractRegularDetails, CodeBlock> regularInvoke(SimpleRegularGoalContext goal) {
    return regularDetailsCases(
        constructor -> constructorCall(goal.description, constructor),
        staticMethod -> staticCall(goal.description, staticMethod),
        instanceMethod -> instanceCall(goal.description, instanceMethod));
  }

  private static CodeBlock constructorCall(SimpleRegularGoalDescription description,
                                           DtoGoalDetails.ConstructorGoalDetails details) {
    TypeName type = details.type();
    ParameterSpec varGoal = parameterSpec(type,
        '_' + downcase(simpleName(type)));
    CodeBlock.Builder builder = CodeBlock.builder();
    if (details.lifecycle == REUSE_INSTANCES) {
      builder.addStatement("this._currently_in_use = false");
    }
    CodeBlock args = description.invocationParameters();
    builder.addStatement("$T $N = new $T($L)", varGoal.type, varGoal, type, args);
    if (details.lifecycle == REUSE_INSTANCES) {
      builder.add(free(description.parameters));
    }
    return builder.addStatement("return $N", varGoal).build();
  }

  private static CodeBlock instanceCall(SimpleRegularGoalDescription description,
                                        DtoGoalDetails.InstanceMethodGoalDetails details) {
    TypeName type = details.goalType;
    String method = details.methodName;
    ParameterSpec varGoal = parameterSpec(type,
        '_' + downcase(simpleName(type)));
    CodeBlock.Builder builder = CodeBlock.builder();
    if (details.lifecycle == REUSE_INSTANCES) {
      builder.addStatement("this._currently_in_use = false");
    }
    if (VOID.equals(type)) {
      builder.addStatement("this.$N.$N($L)", instanceField(description),
          method, description.invocationParameters());
    } else {
      builder.addStatement("$T $N = this.$N.$N($L)", varGoal.type, varGoal, instanceField(description),
          method, description.invocationParameters());
    }
    if (details.lifecycle == REUSE_INSTANCES) {
      builder.addStatement("this.$N = null", instanceField(description));
      builder.add(free(description.parameters));
    }
    if (!VOID.equals(type)) {
      builder.addStatement("return $N", varGoal);
    }
    return builder.build();
  }

  private static CodeBlock staticCall(SimpleRegularGoalDescription description,
                                      DtoGoalDetails.StaticMethodGoalDetails details) {
    TypeName type = details.goalType;
    String method = details.methodName;
    ParameterSpec varGoal = parameterSpec(type,
        '_' + downcase(simpleName(type)));
    CodeBlock.Builder builder = CodeBlock.builder();
    if (details.lifecycle == REUSE_INSTANCES) {
      builder.addStatement("this._currently_in_use = false");
    }
    if (VOID.equals(type)) {
      builder.addStatement("$T.$N($L)", rawClassName(description.context.type),
          method, description.invocationParameters());
    } else {
      builder.addStatement("$T $N = $T.$N($L)", varGoal.type, varGoal,
          rawClassName(description.context.type),
          method, description.invocationParameters());
    }
    if (details.lifecycle == REUSE_INSTANCES) {
      builder.add(free(description.parameters));
    }
    if (!VOID.equals(type)) {
      builder.addStatement("return $N", varGoal);
    }
    return builder.build();
  }

  private static CodeBlock free(List<SimpleParameter> steps) {
    return steps.stream()
        .limit(steps.size() - 1)
        .filter(parameter -> !parameter.type.isPrimitive())
        .map(parameter -> statement("this.$N = null", parameter.name))
        .collect(joinCodeBlocks);
  }

  private Builder() {
    throw new UnsupportedOperationException("no instances");
  }
}
