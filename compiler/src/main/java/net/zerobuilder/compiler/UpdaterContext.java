package net.zerobuilder.compiler;

import com.google.common.collect.ImmutableList;
import com.squareup.javapoet.ClassName;
import com.squareup.javapoet.FieldSpec;
import com.squareup.javapoet.MethodSpec;
import com.squareup.javapoet.ParameterSpec;
import com.squareup.javapoet.TypeName;

import javax.lang.model.element.Name;

import static com.google.auto.common.MoreElements.asType;
import static com.squareup.javapoet.MethodSpec.constructorBuilder;
import static com.squareup.javapoet.MethodSpec.methodBuilder;
import static javax.lang.model.element.ElementKind.CONSTRUCTOR;
import static javax.lang.model.element.Modifier.PRIVATE;
import static javax.lang.model.element.Modifier.PUBLIC;

final class UpdaterContext {

  private static final String UPDATER_IMPL = "UpdaterImpl";

  private final MyContext context;

  UpdaterContext(MyContext context) {
    this.context = context;
  }

  ClassName name() {
    return context.generatedTypeName().nestedClass(UPDATER_IMPL);
  }

  ImmutableList<FieldSpec> fields() {
    ImmutableList.Builder<FieldSpec> builder = ImmutableList.builder();
    for (StepSpec stepSpec : context.stepSpecs) {
      String name = stepSpec.argument.getSimpleName().toString();
      builder.add(FieldSpec.builder(TypeName.get(stepSpec.argument.asType()), name, PRIVATE).build());
    }
    return builder.build();
  }

  MethodSpec constructor() {
    return constructorBuilder()
        .addModifiers(PRIVATE)
        .build();
  }

  ImmutableList<MethodSpec> updaterMethods() {
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

  MethodSpec buildMethod() {
    ClassName targetType = ClassName.get(asType(context.annotatedType));
    MethodSpec.Builder builder = methodBuilder("build")
        .addAnnotation(Override.class)
        .addModifiers(PUBLIC)
        .returns(targetType);
    Name simpleName = context.annotatedExecutable.getSimpleName();
    return (context.annotatedExecutable.getKind() == CONSTRUCTOR
        ? builder.addStatement("return new $T($L)", targetType, context.factoryCallArgs())
        : builder.addStatement("return $T.$N($L)", targetType, simpleName, context.factoryCallArgs()))
        .build();
  }

}
