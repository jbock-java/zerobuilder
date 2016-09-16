package net.zerobuilder.compiler;

import com.google.common.collect.ImmutableList;
import com.squareup.javapoet.ClassName;
import com.squareup.javapoet.MethodSpec;
import com.squareup.javapoet.ParameterSpec;
import com.squareup.javapoet.TypeName;
import com.squareup.javapoet.TypeSpec;
import net.zerobuilder.compiler.ProjectionValidator.ValidParameter;
import net.zerobuilder.compiler.ProjectionValidator.ValidParameter.AccessorPair;
import net.zerobuilder.compiler.ProjectionValidator.ValidParameter.Parameter;

import static com.squareup.javapoet.MethodSpec.methodBuilder;
import static com.squareup.javapoet.TypeSpec.interfaceBuilder;
import static javax.lang.model.element.Modifier.ABSTRACT;
import static javax.lang.model.element.Modifier.PUBLIC;

abstract class ParameterContext {

  final ClassName typeThisStep;
  final TypeName typeNextStep;

  abstract static class ParameterCases<R> {
    abstract R regularParameter(ClassName typeName, TypeName returnType, Parameter parameter, ImmutableList<TypeName> declaredExceptions);
    abstract R beansParameter(ClassName typeName, TypeName returnType, AccessorPair parameter);
  }

  abstract <R> R accept(ParameterCases<R> cases);

  ParameterContext(ClassName typeName, TypeName typeNextStep) {
    this.typeThisStep = typeName;
    this.typeNextStep = typeNextStep;
  }

  final static class RegularParameterContext extends ParameterContext {
    final Parameter parameter;
    final ImmutableList<TypeName> declaredExceptions;
    RegularParameterContext(ClassName typeName, TypeName returnType, Parameter parameter, ImmutableList<TypeName> declaredExceptions) {
      super(typeName, returnType);
      this.declaredExceptions = declaredExceptions;
      this.parameter = parameter;
    }
    @Override
    <R> R accept(ParameterCases<R> cases) {
      return cases.regularParameter(typeThisStep, typeNextStep, parameter, declaredExceptions);
    }
  }

  final static class BeansParameterContext extends ParameterContext {
    final AccessorPair parameter;
    BeansParameterContext(ClassName typeName, TypeName returnType, AccessorPair parameter) {
      super(typeName, returnType);
      this.parameter = parameter;
    }
    @Override
    <R> R accept(ParameterCases<R> cases) {
      return cases.beansParameter(typeThisStep, typeNextStep, parameter);
    }
  }

  abstract static class ParameterFunction<R> {
    abstract R apply(ClassName typeName, TypeName returnType, ValidParameter parameter, ImmutableList<TypeName> declaredExceptions);
  }

  private static <R> ParameterCases<R> always(final ParameterFunction<R> parameterFunction) {
    return new ParameterCases<R>() {
      @Override
      R regularParameter(ClassName typeName, TypeName returnType, Parameter parameter, ImmutableList<TypeName> declaredExceptions) {
        return parameterFunction.apply(typeName, returnType, parameter, declaredExceptions);
      }
      @Override
      R beansParameter(ClassName typeName, TypeName returnType, AccessorPair parameter) {
        return parameterFunction.apply(typeName, returnType, parameter, ImmutableList.<TypeName>of());
      }
    };
  }

  static ParameterCases<TypeSpec> asStepInterface = always(new ParameterFunction<TypeSpec>() {
    @Override
    TypeSpec apply(ClassName typeName, TypeName returnType, ValidParameter parameter, ImmutableList<TypeName> declaredExceptions) {
      String name = parameter.name;
      TypeName type = parameter.type;
      MethodSpec methodSpec = methodBuilder(name)
          .returns(returnType)
          .addParameter(ParameterSpec.builder(type, name).build())
          .addExceptions(declaredExceptions)
          .addModifiers(PUBLIC, ABSTRACT)
          .build();
      return interfaceBuilder(typeName)
          .addMethod(methodSpec)
          .addModifiers(PUBLIC)
          .build();
    }
  });

}
