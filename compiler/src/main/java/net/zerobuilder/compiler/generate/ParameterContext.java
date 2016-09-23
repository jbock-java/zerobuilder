package net.zerobuilder.compiler.generate;

import com.google.common.base.Function;
import com.google.common.collect.ImmutableList;
import com.squareup.javapoet.ClassName;
import com.squareup.javapoet.CodeBlock;
import com.squareup.javapoet.ParameterizedTypeName;
import com.squareup.javapoet.TypeName;
import com.squareup.javapoet.TypeSpec;
import net.zerobuilder.compiler.analyse.ProjectionValidator.ValidParameter;
import net.zerobuilder.compiler.analyse.ProjectionValidator.ValidParameter.AccessorPair;
import net.zerobuilder.compiler.analyse.ProjectionValidator.ValidParameter.RegularParameter;

import static com.squareup.javapoet.MethodSpec.methodBuilder;
import static com.squareup.javapoet.TypeSpec.interfaceBuilder;
import static com.squareup.javapoet.WildcardTypeName.subtypeOf;
import static javax.lang.model.element.Modifier.ABSTRACT;
import static javax.lang.model.element.Modifier.PUBLIC;
import static net.zerobuilder.compiler.Utilities.iterationVarName;
import static net.zerobuilder.compiler.Utilities.nullCheck;
import static net.zerobuilder.compiler.Utilities.parameterSpec;

public abstract class ParameterContext {

  final ClassName typeThisStep;
  final TypeName typeNextStep;

  abstract static class ParameterCases<R> {
    abstract R parameter(ExecutableParameterContext parameterContext);
    abstract R accessorPair(BeansParameterContext beansParameterContext);
  }

  static <R> ParameterCases<R> parameterCases(final Function<ParameterContext, R> executableFunction,
                                              final Function<BeansParameterContext, R> beansFunction) {
    return new ParameterCases<R>() {
      @Override
      R parameter(ExecutableParameterContext parameterContext) {
        return executableFunction.apply(parameterContext);
      }
      @Override
      R accessorPair(BeansParameterContext beansParameterContext) {
        return beansFunction.apply(beansParameterContext);
      }
    };
  }

  abstract <R> R accept(ParameterCases<R> cases);

  ParameterContext(ClassName typeName, TypeName typeNextStep) {
    this.typeThisStep = typeName;
    this.typeNextStep = typeNextStep;
  }

  public final static class ExecutableParameterContext extends ParameterContext {
    final RegularParameter parameter;
    final ImmutableList<TypeName> declaredExceptions;
    public ExecutableParameterContext(ClassName typeThisStep, TypeName typeNextStep, RegularParameter parameter, ImmutableList<TypeName> declaredExceptions) {
      super(typeThisStep, typeNextStep);
      this.declaredExceptions = declaredExceptions;
      this.parameter = parameter;
    }
    @Override
    <R> R accept(ParameterCases<R> cases) {
      return cases.parameter(this);
    }
  }

  static final ParameterCases<ImmutableList<TypeName>> declaredExceptions
      = new ParameterCases<ImmutableList<TypeName>>() {
    @Override
    ImmutableList<TypeName> parameter(ExecutableParameterContext parameterContext) {
      return parameterContext.declaredExceptions;
    }
    @Override
    ImmutableList<TypeName> accessorPair(BeansParameterContext beansParameterContext) {
      return ImmutableList.of();
    }
  };

  static final ParameterCases<ValidParameter> validParameter
      = new ParameterCases<ValidParameter>() {
    @Override
    ValidParameter parameter(ExecutableParameterContext parameterContext) {
      return parameterContext.parameter;
    }
    @Override
    ValidParameter accessorPair(BeansParameterContext beansParameterContext) {
      return beansParameterContext.accessorPair;
    }
  };

  public final static class BeansParameterContext extends ParameterContext {
    final AccessorPair accessorPair;
    public BeansParameterContext(ClassName typeThisStep, TypeName typeNextStep, AccessorPair accessorPair) {
      super(typeThisStep, typeNextStep);
      this.accessorPair = accessorPair;
    }
    @Override
    <R> R accept(ParameterCases<R> cases) {
      return cases.accessorPair(this);
    }
  }

  private static <R> ParameterCases<R> always(final Function<ParameterContext, R> parameterFunction) {
    return new ParameterCases<R>() {
      @Override
      R parameter(ExecutableParameterContext context) {
        return parameterFunction.apply(context);
      }
      @Override
      R accessorPair(BeansParameterContext context) {
        return parameterFunction.apply(context);
      }
    };
  }

  static final ParameterCases<CodeBlock> maybeNullCheck
      = always(new Function<ParameterContext, CodeBlock>() {
    @Override
    public CodeBlock apply(ParameterContext context) {
      ValidParameter parameter = context.accept(validParameter);
      if (!parameter.nonNull || parameter.type.isPrimitive()) {
        return CodeBlock.of("");
      }
      return nullCheck(parameter.name, parameter.name);
    }
  });

  static final ParameterCases<CodeBlock> maybeIterationNullCheck
      = always(new Function<ParameterContext, CodeBlock>() {
    @Override
    public CodeBlock apply(ParameterContext context) {
      ValidParameter parameter = context.accept(validParameter);
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

  private static final Function<ParameterContext, TypeSpec> regularStepInterface
      = new Function<ParameterContext, TypeSpec>() {
    @Override
    public TypeSpec apply(ParameterContext context) {
      ValidParameter parameter = context.accept(validParameter);
      String name = parameter.name;
      TypeName type = parameter.type;
      return interfaceBuilder(context.typeThisStep)
          .addMethod(methodBuilder(name)
              .returns(context.typeNextStep)
              .addParameter(parameterSpec(type, name))
              .addExceptions(context.accept(declaredExceptions))
              .addModifiers(PUBLIC, ABSTRACT)
              .build())
          .addModifiers(PUBLIC)
          .build();
    }
  };

  private static final Function<BeansParameterContext, TypeSpec> beansStepInterface
      = new Function<BeansParameterContext, TypeSpec>() {
    @Override
    public TypeSpec apply(BeansParameterContext context) {
      AccessorPair parameter = context.accessorPair;
      String name = parameter.name;
      if (parameter.collectionType.type.isPresent()) {
        TypeName collectionType = parameter.collectionType.type.get();
        ParameterizedTypeName iterable = ParameterizedTypeName.get(ClassName.get(Iterable.class),
            subtypeOf(collectionType));
        TypeSpec.Builder builder = interfaceBuilder(context.typeThisStep)
            .addModifiers(PUBLIC)
            .addMethod(methodBuilder(name)
                .addParameter(parameterSpec(iterable, name))
                .returns(context.typeNextStep)
                .addModifiers(PUBLIC, ABSTRACT)
                .build())
            .addMethod(methodBuilder(name)
                .returns(context.typeNextStep)
                .addModifiers(PUBLIC, ABSTRACT)
                .build());
        if (parameter.collectionType.allowShortcut) {
          builder.addMethod(methodBuilder(name)
              .addParameter(parameterSpec(collectionType, name))
              .returns(context.typeNextStep)
              .addModifiers(PUBLIC, ABSTRACT)
              .build());
        }
        return builder.build();
      } else {
        return regularStepInterface.apply(context);
      }
    }
  };

  static final Function<ParameterContext, TypeSpec> asStepInterface
      = asFunction(parameterCases(regularStepInterface, beansStepInterface));

}
