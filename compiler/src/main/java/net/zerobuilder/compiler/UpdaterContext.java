package net.zerobuilder.compiler;

import com.google.common.base.Optional;
import com.google.common.collect.ImmutableList;
import com.squareup.javapoet.ClassName;
import com.squareup.javapoet.FieldSpec;
import com.squareup.javapoet.MethodSpec;
import com.squareup.javapoet.TypeSpec;


import static com.google.common.base.Optional.absent;
import static com.squareup.javapoet.MethodSpec.constructorBuilder;
import static com.squareup.javapoet.MethodSpec.methodBuilder;
import static com.squareup.javapoet.TypeSpec.classBuilder;
import static javax.lang.model.element.Modifier.FINAL;
import static javax.lang.model.element.Modifier.PRIVATE;
import static javax.lang.model.element.Modifier.PUBLIC;
import static javax.lang.model.element.Modifier.STATIC;
import static net.zerobuilder.compiler.UberGoalContext.GoalKind.CONSTRUCTOR;
import static net.zerobuilder.compiler.Utilities.downcase;

final class UpdaterContext {

  private static final String UPDATER_IMPL = "UpdaterImpl";

  private final GoalContext context;

  UpdaterContext(GoalContext context) {
    this.context = context;
  }

  ClassName typeName() {
    return context.builderType.nestedClass(UPDATER_IMPL);
  }

  private ImmutableList<FieldSpec> fields() {
    ImmutableList.Builder<FieldSpec> builder = ImmutableList.builder();
    Optional<ClassName> receiver = context.receiverType();
    if (receiver.isPresent()) {
      ClassName r = receiver.get();
      builder.add(FieldSpec.builder(r, "_" + downcase(r.simpleName()), PRIVATE).build());
    }
    for (ParameterContext parameter : context.goalParameters) {
      String name = parameter.name;
      builder.add(FieldSpec.builder(parameter.type, name, PRIVATE).build());
    }
    return builder.build();
  }

  private ImmutableList<MethodSpec> updaterMethods() {
    ImmutableList.Builder<MethodSpec> builder = ImmutableList.builder();
    for (ParameterContext parameter : context.goalParameters) {
      builder.add(methodBuilder(parameter.name)
          .addAnnotation(Override.class)
          .returns(context.contractUpdaterName())
          .addParameter(parameter.parameter())
          .addStatement("this.$N = $N", parameter.name, parameter.name)
          .addStatement("return this")
          .addModifiers(PUBLIC)
          .build());
    }
    return builder.build();
  }

  private MethodSpec buildMethod() {
    MethodSpec.Builder builder = methodBuilder("build")
        .addAnnotation(Override.class)
        .addExceptions(context.thrownTypes)
        .addModifiers(PUBLIC)
        .returns(context.goalType);
    String goal = context.methodName;
    Optional<ClassName> receiver = context.receiverType();
    return (context.kind == CONSTRUCTOR
        ? builder.addStatement("return new $T($L)", context.goalType, context.methodParameters)
        : receiver.isPresent()
        ? builder.addStatement("return $N.$N($L)", "_" + downcase(receiver.get().simpleName()), goal, context.methodParameters)
        : builder.addStatement("return $T.$N($L)", context.config.annotatedType, goal, context.methodParameters))
        .build();
  }

  Optional<TypeSpec> buildUpdaterImpl() {
    if (!context.toBuilder) {
      return absent();
    }
    return Optional.of(classBuilder(typeName())
        .addSuperinterface(context.contractUpdaterName())
        .addFields(fields())
        .addMethods(updaterMethods())
        .addMethod(buildMethod())
        .addModifiers(FINAL, STATIC)
        .addMethod(constructorBuilder().addModifiers(PRIVATE).build())
        .build());
  }

}
