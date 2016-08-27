package isobuilder.compiler;

import com.squareup.javapoet.*;

import javax.lang.model.element.VariableElement;

import static com.squareup.javapoet.MethodSpec.methodBuilder;
import static com.squareup.javapoet.TypeSpec.interfaceBuilder;
import static isobuilder.compiler.Util.upcase;
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

  TypeSpec asInterface() {
    MethodSpec methodSpec = methodBuilder(argument.getSimpleName().toString())
        .returns(returnType)
        .addParameter(asParameter())
        .addModifiers(PUBLIC, ABSTRACT)
        .build();
    return interfaceBuilder(stepName)
        .addMethod(methodSpec)
        .addModifiers(PUBLIC)
        .build();
  }

  ParameterSpec asParameter() {
    return ParameterSpec
        .builder(TypeName.get(argument.asType()), argument.getSimpleName().toString())
        .build();
  }

  MethodSpec asUpdaterMethod(ClassName updaterName) {
    return methodBuilder(argument.getSimpleName().toString())
        .returns(updaterName)
        .addParameter(asParameter())
        .addModifiers(PUBLIC, ABSTRACT)
        .build();
  }

}
