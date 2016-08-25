package isobuilder.compiler;

import com.google.auto.value.AutoValue;
import com.squareup.javapoet.*;

import javax.lang.model.element.VariableElement;

import static com.squareup.javapoet.MethodSpec.methodBuilder;
import static com.squareup.javapoet.TypeSpec.interfaceBuilder;
import static javax.lang.model.element.Modifier.ABSTRACT;
import static javax.lang.model.element.Modifier.PUBLIC;

@AutoValue
abstract class StepSpec {

  abstract ClassName stepName();
  abstract VariableElement argument();
  abstract ClassName returnType();

  static StepSpec stepSpec(ClassName stepName, VariableElement argument, ClassName returnType) {
    return new AutoValue_StepSpec(stepName, argument, returnType);
  }

  final TypeSpec typeSpec() {
    MethodSpec methodSpec = methodBuilder(argument().getSimpleName().toString())
        .returns(returnType())
        .addParameter(TypeName.get(argument().asType()), argument().getSimpleName().toString())
        .addModifiers(PUBLIC, ABSTRACT)
        .build();
    return interfaceBuilder(stepName())
        .addMethod(methodSpec)
        .build();
  }

  final ParameterSpec asParameter() {
    return ParameterSpec.builder(TypeName.get(argument().asType()), argument().getSimpleName().toString()).build();
  }

}
