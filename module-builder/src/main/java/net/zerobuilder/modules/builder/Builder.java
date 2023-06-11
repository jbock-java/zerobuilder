package net.zerobuilder.modules.builder;

import io.jbock.javapoet.CodeBlock;
import io.jbock.javapoet.FieldSpec;
import io.jbock.javapoet.MethodSpec;
import io.jbock.javapoet.ParameterSpec;
import io.jbock.javapoet.TypeName;
import net.zerobuilder.compiler.generate.DtoGoalDetails;
import net.zerobuilder.compiler.generate.DtoGoalDetails.AbstractRegularDetails;
import net.zerobuilder.compiler.generate.DtoRegularGoalDescription.SimpleRegularGoalDescription;
import net.zerobuilder.compiler.generate.DtoRegularParameter.SimpleParameter;

import java.util.ArrayList;
import java.util.List;
import java.util.function.BiFunction;
import java.util.function.Function;
import java.util.function.IntFunction;

import static io.jbock.javapoet.MethodSpec.methodBuilder;
import static io.jbock.javapoet.TypeName.BOOLEAN;
import static io.jbock.javapoet.TypeName.VOID;
import static java.util.Collections.emptyList;
import static javax.lang.model.element.Modifier.PRIVATE;
import static javax.lang.model.element.Modifier.PUBLIC;
import static net.zerobuilder.compiler.generate.DtoContext.ContextLifecycle.REUSE_INSTANCES;
import static net.zerobuilder.compiler.generate.DtoGoalDetails.isInstance;
import static net.zerobuilder.compiler.generate.DtoGoalDetails.regularDetailsCases;
import static net.zerobuilder.compiler.generate.ZeroUtil.downcase;
import static net.zerobuilder.compiler.generate.ZeroUtil.fieldSpec;
import static net.zerobuilder.compiler.generate.ZeroUtil.joinCodeBlocks;
import static net.zerobuilder.compiler.generate.ZeroUtil.parameterSpec;
import static net.zerobuilder.compiler.generate.ZeroUtil.rawClassName;
import static net.zerobuilder.compiler.generate.ZeroUtil.simpleName;
import static net.zerobuilder.compiler.generate.ZeroUtil.statement;
import static net.zerobuilder.compiler.generate.ZeroUtil.upcase;
import static net.zerobuilder.modules.builder.Generator.instanceField;

final class Builder {

  static final String IN_USE = "_currently_in_use";

  static TypeName nextType(int i, SimpleRegularGoalDescription description) {
    if (i < description.parameters.size() - 1) {
      return description.context.generatedType
          .nestedClass(upcase(description.details.name() + "Builder"))
          .nestedClass(upcase(description.parameters.get(i + 1).name));
    }
    return description.details.type();
  }

  static final Function<SimpleRegularGoalDescription, List<FieldSpec>> fields
      = description -> {
    List<SimpleParameter> steps = description.parameters;
    ArrayList<FieldSpec> builder = new ArrayList<>(steps.size() + 2);
    if (isInstance.apply(description.details)) {
      builder.add(instanceField(description));
    }
    if (description.details.lifecycle == REUSE_INSTANCES) {
      builder.add(fieldSpec(BOOLEAN, IN_USE, PRIVATE));
    }
    steps.stream()
        .limit(steps.size() - 1)
        .map(parameter -> fieldSpec(parameter.type, parameter.name, PRIVATE))
        .forEach(builder::add);
    return builder;
  };

  static IntFunction<MethodSpec> steps(SimpleRegularGoalDescription description) {
    return i -> {
      SimpleParameter step = description.parameters.get(i);
      TypeName type = step.type;
      String name = step.name;
      ParameterSpec parameter = parameterSpec(type, name);
      List<TypeName> thrownTypes = i < description.parameters.size() - 1 ?
          emptyList() :
          description.thrownTypes;
      TypeName nextType = nextType(i, description);
      return methodBuilder(step.name)
          .addAnnotation(Override.class)
          .addParameter(parameter)
          .returns(nextType)
          .addCode(normalAssignment(i, description))
          .addModifiers(PUBLIC)
          .addExceptions(thrownTypes)
          .build();
    };
  }

  private static CodeBlock normalAssignment(int i, SimpleRegularGoalDescription description) {
    SimpleParameter step = description.parameters.get(i);
    TypeName type = step.type;
    String name = step.name;
    ParameterSpec parameter = parameterSpec(type, name);
    if (i == description.parameters.size() - 1) {
      return regularInvoke.apply(description.details, description);
    } else {
      return CodeBlock.builder()
          .addStatement("this.$N = $N", fieldSpec(step.type, step.name), parameter)
          .addStatement("return this")
          .build();
    }
  }

  private static final BiFunction<AbstractRegularDetails, SimpleRegularGoalDescription, CodeBlock> regularInvoke =
      regularDetailsCases(
          (constructor, description) -> constructorCall(description, constructor),
          (staticMethod, description) -> staticCall(description, staticMethod),
          (instanceMethod, description) -> instanceCall(description, instanceMethod));

  private static CodeBlock constructorCall(SimpleRegularGoalDescription description,
                                           DtoGoalDetails.ConstructorGoalDetails details) {
    TypeName type = details.type();
    ParameterSpec varGoal = parameterSpec(type,
        '_' + downcase(simpleName(type)));
    CodeBlock.Builder builder = CodeBlock.builder();
    if (details.lifecycle == REUSE_INSTANCES) {
      builder.addStatement("this.$L = $L", IN_USE, false);
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
      builder.addStatement("this.$L = $L", IN_USE, false);
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
      builder.addStatement("this.$L = $L", IN_USE, false);
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
