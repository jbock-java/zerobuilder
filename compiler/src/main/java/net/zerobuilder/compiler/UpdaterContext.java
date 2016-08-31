package net.zerobuilder.compiler;

import com.google.common.base.Optional;
import com.google.common.collect.ImmutableList;
import com.squareup.javapoet.ClassName;
import com.squareup.javapoet.FieldSpec;
import com.squareup.javapoet.MethodSpec;
import com.squareup.javapoet.ParameterSpec;
import com.squareup.javapoet.TypeName;
import com.squareup.javapoet.TypeSpec;

import javax.lang.model.element.Name;

import static com.google.common.base.Optional.absent;
import static com.squareup.javapoet.MethodSpec.constructorBuilder;
import static com.squareup.javapoet.MethodSpec.methodBuilder;
import static com.squareup.javapoet.TypeSpec.classBuilder;
import static javax.lang.model.element.ElementKind.CONSTRUCTOR;
import static javax.lang.model.element.Modifier.FINAL;
import static javax.lang.model.element.Modifier.PRIVATE;
import static javax.lang.model.element.Modifier.PUBLIC;
import static javax.lang.model.element.Modifier.STATIC;

final class UpdaterContext {

  private static final String UPDATER_IMPL = "UpdaterImpl";

  private final MyContext context;

  UpdaterContext(MyContext context) {
    this.context = context;
  }

  ClassName typeName() {
    return context.generatedTypeName().nestedClass(UPDATER_IMPL);
  }

  private ImmutableList<FieldSpec> fields() {
    ImmutableList.Builder<FieldSpec> builder = ImmutableList.builder();
    for (StepSpec stepSpec : context.stepSpecs) {
      String name = stepSpec.argument.getSimpleName().toString();
      builder.add(FieldSpec.builder(TypeName.get(stepSpec.argument.asType()), name, PRIVATE).build());
    }
    return builder.build();
  }

  private ImmutableList<MethodSpec> updaterMethods() {
    ImmutableList.Builder<MethodSpec> builder = ImmutableList.builder();
    for (StepSpec stepSpec : context.stepSpecs) {
      ParameterSpec parameter = stepSpec.parameter();
      builder.add(methodBuilder(parameter.name)
          .addAnnotation(Override.class)
          .returns(context.contractUpdaterName())
          .addParameter(parameter)
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
        .addExceptions(context.thrownTypes())
        .addModifiers(PUBLIC)
        .returns(context.goalType);
    Name simpleName = context.buildVia.getSimpleName();
    return (context.buildVia.getKind() == CONSTRUCTOR
        ? builder.addStatement("return new $T($L)", context.goalType, context.factoryCallArgs())
        : builder.addStatement("return $T.$N($L)", context.goalType, simpleName, context.factoryCallArgs()))
        .build();
  }

  Optional<TypeSpec> buildUpdaterImpl() {
    if (!context.toBuilder()) {
      return absent();
    }
    UpdaterContext updaterContext = context.updaterContext();
    return Optional.of(classBuilder(updaterContext.typeName())
        .addSuperinterface(context.contractUpdaterName())
        .addFields(updaterContext.fields())
        .addMethods(updaterContext.updaterMethods())
        .addMethod(updaterContext.buildMethod())
        .addModifiers(FINAL, STATIC)
        .addMethod(constructorBuilder().addModifiers(PRIVATE).build())
        .build());
  }


}
