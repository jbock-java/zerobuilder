package net.zerobuilder.compiler;

import com.google.common.base.Optional;
import com.google.common.collect.ImmutableList;
import com.squareup.javapoet.ClassName;
import com.squareup.javapoet.CodeBlock;
import com.squareup.javapoet.FieldSpec;
import com.squareup.javapoet.MethodSpec;
import com.squareup.javapoet.ParameterSpec;
import com.squareup.javapoet.TypeName;
import com.squareup.javapoet.TypeSpec;
import net.zerobuilder.compiler.GoalContext.SharedGoalContext;

import javax.lang.model.element.Name;

import static com.google.common.collect.Iterables.getLast;
import static com.squareup.javapoet.MethodSpec.constructorBuilder;
import static com.squareup.javapoet.MethodSpec.methodBuilder;
import static com.squareup.javapoet.TypeSpec.classBuilder;
import static javax.lang.model.element.ElementKind.CONSTRUCTOR;
import static javax.lang.model.element.Modifier.FINAL;
import static javax.lang.model.element.Modifier.PRIVATE;
import static javax.lang.model.element.Modifier.PUBLIC;
import static javax.lang.model.element.Modifier.STATIC;
import static net.zerobuilder.compiler.Utilities.downcase;

final class StepsContext {

  private final SharedGoalContext context;

  StepsContext(SharedGoalContext context) {
    this.context = context;
  }

  private ImmutableList<FieldSpec> fields() {
    ImmutableList.Builder<FieldSpec> builder = ImmutableList.builder();
    Optional<ClassName> receiver = context.receiverType();
    if (receiver.isPresent()) {
      ClassName r = receiver.get();
      builder.add(FieldSpec.builder(r, "_" + downcase(r.simpleName()), PRIVATE).build());
    }
    for (ParameterContext parameter : context.parameters.subList(0, context.parameters.size() - 1)) {
      String name = parameter.name;
      builder.add(FieldSpec.builder(parameter.type, name, PRIVATE).build());
    }
    return builder.build();
  }

  private MethodSpec constructor() {
    return constructorBuilder()
        .addModifiers(PRIVATE)
        .build();
  }

  private ImmutableList<MethodSpec> stepsButLast() {
    ImmutableList.Builder<MethodSpec> builder = ImmutableList.builder();
    for (ParameterContext parameter : context.parameters.subList(0, context.parameters.size() - 1)) {
      builder.add(methodBuilder(parameter.name)
          .addAnnotation(Override.class)
          .returns(parameter.returnType)
          .addParameter(parameter.parameter())
          .addStatement("this.$N = $N", parameter.name, parameter.name)
          .addStatement("return this")
          .addModifiers(PUBLIC)
          .build());
    }
    return builder.build();
  }

  private MethodSpec lastStep() {
    ParameterContext parameter = getLast(context.parameters);
    MethodSpec.Builder builder = methodBuilder(parameter.name)
        .addAnnotation(Override.class)
        .addParameter(parameter.parameter())
        .addExceptions(context.thrownTypes())
        .addModifiers(PUBLIC)
        .returns(context.goalType);
    Name goal = context.goal.getSimpleName();
    String returnLiteral = TypeName.VOID.equals(context.goalType) ? "" : "return ";
    Optional<ClassName> receiver = context.receiverType();
    CodeBlock parameters = context.goalParameters;
    return (context.goal.getKind() == CONSTRUCTOR
        ? builder.addStatement("return new $T($L)", context.goalType, parameters)
        : receiver.isPresent()
        ? builder.addStatement("$L$N.$N($L)", returnLiteral, "_" + downcase(receiver.get().simpleName()), goal, parameters)
        : builder.addStatement("$L$T.$N($L)", returnLiteral, context.config.annotatedType, goal, parameters))
        .build();
  }

  TypeSpec buildStepsImpl() {
    return classBuilder(context.stepsImplTypeName())
        .addSuperinterfaces(context.stepInterfaceNames())
        .addFields(fields())
        .addMethod(constructor())
        .addMethods(stepsButLast())
        .addMethod(lastStep())
        .addModifiers(FINAL, STATIC)
        .build();
  }

}
