package isobuilder.compiler;

import com.squareup.javapoet.*;

import javax.lang.model.element.Modifier;
import javax.lang.model.element.VariableElement;

import static com.squareup.javapoet.MethodSpec.methodBuilder;
import static com.squareup.javapoet.TypeSpec.interfaceBuilder;
import static javax.lang.model.element.Modifier.ABSTRACT;
import static javax.lang.model.element.Modifier.PUBLIC;

final class StepSpec {

  final ClassName stepName;
  final VariableElement argument;
  final ClassName returnType;

  private StepSpec(ClassName stepName, VariableElement argument, ClassName returnType) {
    this.stepName = stepName;
    this.argument = argument;
    this.returnType = returnType;
  }

  static StepSpec stepSpec(ClassName stepName, VariableElement argument, ClassName returnType) {
    return new StepSpec(stepName, argument, returnType);
  }

  TypeSpec asInterface(Modifier[] modifiers) {
    MethodSpec methodSpec = methodBuilder(argument.getSimpleName().toString())
        .returns(returnType)
        .addParameter(parameter())
        .addModifiers(PUBLIC, ABSTRACT)
        .build();
    return interfaceBuilder(stepName)
        .addMethod(methodSpec)
        .addModifiers(modifiers)
        .build();
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
