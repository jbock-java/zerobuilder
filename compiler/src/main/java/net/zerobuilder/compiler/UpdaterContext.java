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
import static net.zerobuilder.compiler.UberGoalContext.GoalKind.INSTANCE_METHOD;
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
    if (context.kind == INSTANCE_METHOD) {
      ClassName receiverType = context.config.annotatedType;
      builder.add(FieldSpec.builder(receiverType, '_' + downcase(receiverType.simpleName()), PRIVATE).build());
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
    return methodBuilder("build")
        .addAnnotation(Override.class)
        .addExceptions(context.thrownTypes)
        .addModifiers(PUBLIC)
        .returns(context.goalType)
        .addCode(context.goalCall).build();
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
