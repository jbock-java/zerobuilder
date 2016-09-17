package net.zerobuilder.compiler;

import com.google.common.base.Function;
import com.google.common.collect.ImmutableList;
import com.squareup.javapoet.ClassName;
import com.squareup.javapoet.ParameterSpec;
import com.squareup.javapoet.ParameterizedTypeName;
import com.squareup.javapoet.TypeName;
import com.squareup.javapoet.TypeSpec;
import net.zerobuilder.compiler.ProjectionValidator.ValidParameter;
import net.zerobuilder.compiler.ProjectionValidator.ValidParameter.AccessorPair;
import net.zerobuilder.compiler.ProjectionValidator.ValidParameter.Parameter;

import static com.squareup.javapoet.MethodSpec.methodBuilder;
import static com.squareup.javapoet.TypeSpec.interfaceBuilder;
import static javax.lang.model.element.Modifier.ABSTRACT;
import static javax.lang.model.element.Modifier.PUBLIC;
import static net.zerobuilder.compiler.Utilities.parameterSpec;

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

  static <R> Function<ParameterContext, R> asFunction(final ParameterCases<R> cases) {
    return new Function<ParameterContext, R>() {
      @Override
      public R apply(ParameterContext parameterContext) {
        return parameterContext.accept(cases);
      }
    };
  }

  static Function<ParameterContext, TypeSpec> asStepInterface = asFunction(new ParameterCases<TypeSpec>() {
    @Override
    TypeSpec regularParameter(ClassName typeName, TypeName returnType, Parameter parameter, ImmutableList<TypeName> declaredExceptions) {
      return regularStepInterface(typeName, returnType, parameter, declaredExceptions);
    }
    @Override
    TypeSpec beansParameter(ClassName typeName, TypeName returnType, AccessorPair parameter) {
      String name = parameter.name;
      if (parameter.setterlessCollection.isPresent()) {
        ClassName collectionType = parameter.setterlessCollection.get();
        ParameterizedTypeName iterable = ParameterizedTypeName.get(ClassName.get(Iterable.class), collectionType);
        return interfaceBuilder(typeName)
            .addMethod(methodBuilder(name)
                .returns(returnType)
                .addParameter(parameterSpec(iterable, name))
                .addModifiers(PUBLIC, ABSTRACT)
                .build())
            .addMethod(methodBuilder(name)
                .returns(returnType)
                .addParameter(parameterSpec(collectionType, name))
                .addModifiers(PUBLIC, ABSTRACT)
                .build())
            .addModifiers(PUBLIC)
            .build();
      } else {
        return regularStepInterface(typeName, returnType, parameter, ImmutableList.<TypeName>of());
      }
    }
  });

  private static TypeSpec regularStepInterface(ClassName typeName, TypeName returnType, ValidParameter parameter, ImmutableList<TypeName> declaredExceptions) {
    String name = parameter.name;
    TypeName type = parameter.type;
    return interfaceBuilder(typeName)
        .addMethod(methodBuilder(name)
            .returns(returnType)
            .addParameter(parameterSpec(type, name))
            .addExceptions(declaredExceptions)
            .addModifiers(PUBLIC, ABSTRACT)
            .build())
        .addModifiers(PUBLIC)
        .build();
  }

}
