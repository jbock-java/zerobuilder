package net.zerobuilder.compiler;

import com.google.common.base.Optional;
import com.google.common.collect.ImmutableList;
import com.squareup.javapoet.ClassName;
import com.squareup.javapoet.MethodSpec;
import com.squareup.javapoet.ParameterSpec;
import com.squareup.javapoet.TypeName;
import com.squareup.javapoet.TypeSpec;
import net.zerobuilder.compiler.ToBuilderValidator.ProjectionInfo;

import javax.lang.model.element.Modifier;
import javax.lang.model.element.VariableElement;
import java.util.Set;

import static com.google.common.collect.Iterables.toArray;
import static com.squareup.javapoet.MethodSpec.methodBuilder;
import static com.squareup.javapoet.TypeSpec.interfaceBuilder;
import static javax.lang.model.element.Modifier.ABSTRACT;
import static javax.lang.model.element.Modifier.PUBLIC;

final class ParameterContext {

  final ClassName stepContractType;
  final VariableElement parameter;
  final TypeName returnType;
  final Optional<String> projectionMethodName;

  private ParameterContext(ClassName stepContractType, VariableElement parameter, TypeName returnType, Optional<String> projectionMethodName) {
    this.stepContractType = stepContractType;
    this.parameter = parameter;
    this.returnType = returnType;
    this.projectionMethodName = projectionMethodName;
  }

  static ParameterContext stepSpec(ClassName stepName, ProjectionInfo projectionInfo, TypeName returnType) {
    return new ParameterContext(stepName, projectionInfo.parameter, returnType, projectionInfo.projectionMethodName);
  }

  TypeSpec asStepInterface(Set<Modifier> modifiers, ImmutableList<TypeName> declaredExceptions) {
    MethodSpec methodSpec = methodBuilder(parameter.getSimpleName().toString())
        .returns(returnType)
        .addParameter(parameter())
        .addExceptions(declaredExceptions)
        .addModifiers(PUBLIC, ABSTRACT)
        .build();
    return interfaceBuilder(stepContractType)
        .addMethod(methodSpec)
        .addModifiers(toArray(modifiers, Modifier.class))
        .build();
  }

  TypeSpec asStepInterface(Set<Modifier> modifiers) {
    return asStepInterface(modifiers, ImmutableList.<TypeName>of());
  }

  ParameterSpec parameter() {
    return ParameterSpec
        .builder(TypeName.get(parameter.asType()), parameter.getSimpleName().toString())
        .build();
  }

  MethodSpec asUpdaterInterfaceMethod(ClassName updaterName) {
    return methodBuilder(parameter.getSimpleName().toString())
        .returns(updaterName)
        .addParameter(parameter())
        .addModifiers(PUBLIC, ABSTRACT)
        .build();
  }

}
