package net.zerobuilder.compiler;

import com.google.common.collect.ImmutableList;
import com.squareup.javapoet.ClassName;
import com.squareup.javapoet.MethodSpec;
import com.squareup.javapoet.ParameterSpec;
import com.squareup.javapoet.TypeName;
import com.squareup.javapoet.TypeSpec;

import javax.lang.model.element.Modifier;
import javax.lang.model.element.VariableElement;
import java.util.Set;

import static com.google.common.collect.Iterables.toArray;
import static com.squareup.javapoet.MethodSpec.methodBuilder;
import static com.squareup.javapoet.TypeSpec.interfaceBuilder;
import static javax.lang.model.element.Modifier.ABSTRACT;
import static javax.lang.model.element.Modifier.PUBLIC;

final class StepSpec {

  final ClassName stepName;
  final VariableElement argument;
  final TypeName returnType;

  private StepSpec(ClassName stepName, VariableElement argument, TypeName returnType) {
    this.stepName = stepName;
    this.argument = argument;
    this.returnType = returnType;
  }

  static StepSpec stepSpec(ClassName stepName, VariableElement argument, TypeName returnType) {
    return new StepSpec(stepName, argument, returnType);
  }

  TypeSpec asInterface(Set<Modifier> modifiers, ImmutableList<TypeName> thrownTypes) {
    MethodSpec methodSpec = methodBuilder(argument.getSimpleName().toString())
        .returns(returnType)
        .addParameter(parameter())
        .addExceptions(thrownTypes)
        .addModifiers(PUBLIC, ABSTRACT)
        .build();
    return interfaceBuilder(stepName)
        .addMethod(methodSpec)
        .addModifiers(toArray(modifiers, Modifier.class))
        .build();
  }

  TypeSpec asInterface(Set<Modifier> modifiers) {
    return asInterface(modifiers, ImmutableList.<TypeName>of());
  }

  ParameterSpec parameter() {
    return ParameterSpec
        .builder(TypeName.get(argument.asType()), argument.getSimpleName().toString())
        .build();
  }

  MethodSpec asUpdaterInterfaceMethod(ClassName updaterName) {
    return methodBuilder(argument.getSimpleName().toString())
        .returns(updaterName)
        .addParameter(parameter())
        .addModifiers(PUBLIC, ABSTRACT)
        .build();
  }

}
