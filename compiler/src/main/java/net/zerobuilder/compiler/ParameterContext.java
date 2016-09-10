package net.zerobuilder.compiler;

import com.google.common.collect.ImmutableList;
import com.squareup.javapoet.ClassName;
import com.squareup.javapoet.MethodSpec;
import com.squareup.javapoet.ParameterSpec;
import com.squareup.javapoet.TypeName;
import com.squareup.javapoet.TypeSpec;
import net.zerobuilder.compiler.ToBuilderValidator.ValidParameter;

import javax.lang.model.element.Modifier;
import java.util.Set;

import static com.google.common.collect.Iterables.toArray;
import static com.squareup.javapoet.MethodSpec.methodBuilder;
import static com.squareup.javapoet.TypeSpec.interfaceBuilder;
import static javax.lang.model.element.Modifier.ABSTRACT;
import static javax.lang.model.element.Modifier.PUBLIC;

/**
 * <p>method, constructor goal: parameter</p>
 * <p>field goal: setter</p>
 */
final class ParameterContext {

  final ValidParameter validParameter;

  final ClassName stepContract;
  final TypeName returnType;

  ParameterContext(ClassName stepContract, ValidParameter validParameter, TypeName returnType) {
    this.stepContract = stepContract;
    this.validParameter = validParameter;
    this.returnType = returnType;
  }

  TypeSpec asStepInterface(Set<Modifier> modifiers, ImmutableList<TypeName> declaredExceptions) {
    MethodSpec methodSpec = methodBuilder(validParameter.name)
        .returns(returnType)
        .addParameter(parameter())
        .addExceptions(declaredExceptions)
        .addModifiers(PUBLIC, ABSTRACT)
        .build();
    return interfaceBuilder(stepContract)
        .addMethod(methodSpec)
        .addModifiers(toArray(modifiers, Modifier.class))
        .build();
  }

  TypeSpec asStepInterface(Set<Modifier> modifiers) {
    return asStepInterface(modifiers, ImmutableList.<TypeName>of());
  }

  ParameterSpec parameter() {
    return ParameterSpec.builder(validParameter.type, validParameter.name).build();
  }

  MethodSpec asUpdaterInterfaceMethod(ClassName updaterName) {
    return methodBuilder(validParameter.name)
        .returns(updaterName)
        .addParameter(parameter())
        .addModifiers(PUBLIC, ABSTRACT)
        .build();
  }

}
