package net.zerobuilder.compiler;

import com.google.common.base.Function;
import com.google.common.base.Optional;
import com.google.common.collect.ImmutableList;
import com.squareup.javapoet.ClassName;
import com.squareup.javapoet.CodeBlock;
import com.squareup.javapoet.ParameterizedTypeName;
import com.squareup.javapoet.TypeName;
import com.squareup.javapoet.TypeSpec;
import net.zerobuilder.compiler.ProjectionValidator.ValidParameter;
import net.zerobuilder.compiler.ProjectionValidator.ValidParameter.AccessorPair;
import net.zerobuilder.compiler.ProjectionValidator.ValidParameter.RegularParameter;

import static com.squareup.javapoet.MethodSpec.methodBuilder;
import static com.squareup.javapoet.TypeSpec.interfaceBuilder;
import static com.squareup.javapoet.WildcardTypeName.subtypeOf;
import static javax.lang.model.element.Modifier.ABSTRACT;
import static javax.lang.model.element.Modifier.PUBLIC;
import static javax.lang.model.element.Modifier.STATIC;
import static net.zerobuilder.compiler.Utilities.iterationVarName;
import static net.zerobuilder.compiler.Utilities.nullCheck;
import static net.zerobuilder.compiler.Utilities.parameterSpec;

abstract class ParameterContext {

  final ClassName typeThisStep;
  final TypeName typeNextStep;

  abstract static class ParameterCases<R> {
    abstract R parameter(ClassName typeName, TypeName returnType, RegularParameter parameter, ImmutableList<TypeName> declaredExceptions);
    abstract R accessorPair(ClassName typeName, TypeName returnType, AccessorPair parameter);
  }

  abstract <R> R accept(ParameterCases<R> cases);

  ParameterContext(ClassName typeName, TypeName typeNextStep) {
    this.typeThisStep = typeName;
    this.typeNextStep = typeNextStep;
  }

  final static class ExecutableParameterContext extends ParameterContext {
    final RegularParameter parameter;
    final ImmutableList<TypeName> declaredExceptions;
    ExecutableParameterContext(ClassName typeName, TypeName returnType, RegularParameter parameter, ImmutableList<TypeName> declaredExceptions) {
      super(typeName, returnType);
      this.declaredExceptions = declaredExceptions;
      this.parameter = parameter;
    }
    @Override
    <R> R accept(ParameterCases<R> cases) {
      return cases.parameter(typeThisStep, typeNextStep, parameter, declaredExceptions);
    }
  }

  final static class BeansParameterContext extends ParameterContext {
    final AccessorPair accessorPair;
    BeansParameterContext(ClassName typeName, TypeName returnType, AccessorPair accessorPair) {
      super(typeName, returnType);
      this.accessorPair = accessorPair;
    }
    @Override
    <R> R accept(ParameterCases<R> cases) {
      return cases.accessorPair(typeThisStep, typeNextStep, accessorPair);
    }
  }

  abstract static class ParameterFunction<R> {
    abstract R apply(ClassName typeName, TypeName returnType, ValidParameter parameter, ImmutableList<TypeName> declaredExceptions);
  }

  private static <R> ParameterCases<R> always(final ParameterFunction<R> parameterFunction) {
    return new ParameterCases<R>() {
      @Override
      R parameter(ClassName typeName, TypeName returnType, RegularParameter parameter, ImmutableList<TypeName> declaredExceptions) {
        return parameterFunction.apply(typeName, returnType, parameter, declaredExceptions);
      }
      @Override
      R accessorPair(ClassName typeName, TypeName returnType, AccessorPair parameter) {
        return parameterFunction.apply(typeName, returnType, parameter, ImmutableList.<TypeName>of());
      }
    };
  }

  static ParameterCases<CodeBlock> maybeNullCheck = always(new ParameterFunction<CodeBlock>() {
    @Override
    CodeBlock apply(ClassName typeName, TypeName returnType, ValidParameter parameter, ImmutableList<TypeName> declaredExceptions) {
      if (!parameter.nonNull || parameter.type.isPrimitive()) {
        return CodeBlock.of("");
      }
      return nullCheck(parameter.name, parameter.name);
    }
  });

  static ParameterCases<CodeBlock> maybeIterationNullCheck = always(new ParameterFunction<CodeBlock>() {
    @Override
    CodeBlock apply(ClassName typeName, TypeName returnType, ValidParameter parameter, ImmutableList<TypeName> declaredExceptions) {
      if (!parameter.nonNull || parameter.type.isPrimitive()) {
        return CodeBlock.of("");
      }
      return nullCheck(iterationVarName, parameter.name + " (element)");
    }
  });

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
    TypeSpec parameter(ClassName typeName, TypeName returnType, RegularParameter parameter, ImmutableList<TypeName> declaredExceptions) {
      return regularStepInterface(typeName, returnType, parameter, declaredExceptions);
    }
    @Override
    TypeSpec accessorPair(ClassName typeName, TypeName returnType, AccessorPair parameter) {
      String name = parameter.name;
      if (parameter.collectionType.type.isPresent()) {
        TypeName collectionType = parameter.collectionType.type.get();
        ParameterizedTypeName iterable = ParameterizedTypeName.get(ClassName.get(Iterable.class),
            subtypeOf(collectionType));
        TypeSpec.Builder builder = interfaceBuilder(typeName)
            .addModifiers(PUBLIC)
            .addMethod(methodBuilder(name)
                .returns(returnType)
                .addParameter(parameterSpec(iterable, name))
                .addModifiers(PUBLIC, ABSTRACT)
                .build())
            .addMethod(methodBuilder(name)
                .returns(returnType)
                .addModifiers(PUBLIC, ABSTRACT)
                .build());
        if (parameter.collectionType.allowShortcut) {
          builder.addMethod(methodBuilder(name)
              .returns(returnType)
              .addParameter(parameterSpec(collectionType, name))
              .addModifiers(PUBLIC, ABSTRACT)
              .build());
        }
        return builder.build();
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
