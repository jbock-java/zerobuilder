package net.zerobuilder.compiler;

import com.google.common.collect.ImmutableList;
import com.squareup.javapoet.ClassName;
import com.squareup.javapoet.FieldSpec;
import com.squareup.javapoet.MethodSpec;
import com.squareup.javapoet.ParameterSpec;
import com.squareup.javapoet.TypeName;

import javax.lang.model.element.Name;

import static com.google.auto.common.MoreElements.asType;
import static com.google.common.collect.Iterables.getLast;
import static com.squareup.javapoet.MethodSpec.constructorBuilder;
import static com.squareup.javapoet.MethodSpec.methodBuilder;
import static javax.lang.model.element.ElementKind.CONSTRUCTOR;
import static javax.lang.model.element.Modifier.PRIVATE;
import static javax.lang.model.element.Modifier.PUBLIC;

final class StepsContext {

  private static final String STEPS_IMPL = "StepsImpl";

  private final MyContext context;

  StepsContext(MyContext context) {
    this.context = context;
  }

  ClassName typeName() {
    return context.generatedTypeName().nestedClass(STEPS_IMPL);
  }

  ImmutableList<FieldSpec> fields() {
    ImmutableList.Builder<FieldSpec> builder = ImmutableList.builder();
    for (StepSpec stepSpec : context.stepSpecs.subList(0, context.stepSpecs.size() - 1)) {
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

  ImmutableList<MethodSpec> stepsButLast() {
    ImmutableList.Builder<MethodSpec> builder = ImmutableList.builder();
    for (StepSpec stepSpec : context.stepSpecs.subList(0, context.stepSpecs.size() - 1)) {
      ParameterSpec parameter = stepSpec.parameter();
      builder.add(methodBuilder(parameter.name)
          .addAnnotation(Override.class)
          .returns(stepSpec.returnType)
          .addParameter(parameter)
          .addStatement("this.$N = $N", parameter.name, parameter.name)
          .addStatement("return this")
          .addModifiers(PUBLIC)
          .build());
    }
    return builder.build();
  }

  MethodSpec lastStep() {
    StepSpec stepSpec = getLast(context.stepSpecs);
    MethodSpec.Builder builder = methodBuilder(stepSpec.argument.getSimpleName().toString())
        .addAnnotation(Override.class)
        .addParameter(stepSpec.parameter())
        .addModifiers(PUBLIC)
        .returns(context.goalType);
    Name simpleName = context.buildVia.getSimpleName();
    return (context.buildVia.getKind() == CONSTRUCTOR
        ? builder.addStatement("return new $T($L)", context.buildElement, context.factoryCallArgs())
        : builder.addStatement("return $T.$N($L)", context.buildElement, simpleName, context.factoryCallArgs()))
        .build();
  }

}
