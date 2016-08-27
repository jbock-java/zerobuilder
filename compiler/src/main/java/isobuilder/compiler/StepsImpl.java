package isobuilder.compiler;

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
import static isobuilder.compiler.Target.STEPS_IMPL;
import static javax.lang.model.element.ElementKind.CONSTRUCTOR;
import static javax.lang.model.element.Modifier.PRIVATE;
import static javax.lang.model.element.Modifier.PUBLIC;

final class StepsImpl {

  private final Target target;

  StepsImpl(Target target) {
    this.target = target;
  }

  ClassName name() {
    return target.generatedClassName().nestedClass(STEPS_IMPL);
  }

  ImmutableList<FieldSpec> fields() {
    ImmutableList.Builder<FieldSpec> builder = ImmutableList.builder();
    for (StepSpec stepSpec : target.stepSpecs.subList(0, target.stepSpecs.size() - 1)) {
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
    for (StepSpec stepSpec : target.stepSpecs.subList(0, target.stepSpecs.size() - 1)) {
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
    StepSpec stepSpec = getLast(target.stepSpecs);
    ClassName targetType = ClassName.get(asType(target.typeElement));
    MethodSpec.Builder builder = methodBuilder(stepSpec.argument.getSimpleName().toString())
        .addAnnotation(Override.class)
        .addParameter(stepSpec.parameter())
        .addModifiers(PUBLIC)
        .returns(targetType);
    Name simpleName = target.executableElement.getSimpleName();
    return (target.executableElement.getKind() == CONSTRUCTOR
        ? builder.addStatement("return new $T($L)", targetType, target.factoryCallArgs())
        : builder.addStatement("return $T.$N($L)", targetType, simpleName, target.factoryCallArgs()))
        .build();
  }

}
