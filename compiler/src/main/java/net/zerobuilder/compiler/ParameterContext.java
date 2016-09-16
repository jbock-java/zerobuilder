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
abstract class ParameterContext {

  /**
   * Type of the "step" interface that corresponds to this parameter
   */
  final ClassName typeName;
  final TypeName returnType;
  abstract ValidParameter validParameter();

  ParameterContext(ClassName typeName, TypeName returnType) {
    this.typeName = typeName;
    this.returnType = returnType;
  }

  final static class RegularParameterContext extends ParameterContext {
    final ValidParameter.Parameter parameter;
    RegularParameterContext(ClassName typeName, TypeName returnType, ValidParameter.Parameter parameter) {
      super(typeName, returnType);
      this.parameter = parameter;
    }
    @Override
    ValidParameter validParameter() {
      return parameter;
    }
  }

  final static class BeansParameterContext extends ParameterContext {
    final ValidParameter.AccessorPair parameter;
    BeansParameterContext(ClassName typeName, TypeName returnType, ValidParameter.AccessorPair parameter) {
      super(typeName, returnType);
      this.parameter = parameter;
    }
    @Override
    ValidParameter validParameter() {
      return parameter;
    }
  }

  TypeSpec asStepInterface(Set<Modifier> modifiers, ImmutableList<TypeName> declaredExceptions) {
    String name = validParameter().name;
    TypeName type = validParameter().type;
    MethodSpec methodSpec = methodBuilder(name)
        .returns(returnType)
        .addParameter(ParameterSpec.builder(type, name).build())
        .addExceptions(declaredExceptions)
        .addModifiers(PUBLIC, ABSTRACT)
        .build();
    return interfaceBuilder(typeName)
        .addMethod(methodSpec)
        .addModifiers(toArray(modifiers, Modifier.class))
        .build();
  }

  TypeSpec asStepInterface(Set<Modifier> modifiers) {
    return asStepInterface(modifiers, ImmutableList.<TypeName>of());
  }

}
