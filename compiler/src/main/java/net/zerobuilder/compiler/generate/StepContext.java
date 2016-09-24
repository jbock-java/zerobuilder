package net.zerobuilder.compiler.generate;

import com.google.common.base.Function;
import com.google.common.collect.ImmutableList;
import com.squareup.javapoet.ClassName;
import com.squareup.javapoet.CodeBlock;
import com.squareup.javapoet.ParameterizedTypeName;
import com.squareup.javapoet.TypeName;
import com.squareup.javapoet.TypeSpec;
import net.zerobuilder.compiler.analyse.DtoShared;
import net.zerobuilder.compiler.analyse.DtoShared.ValidBeanParameter;
import net.zerobuilder.compiler.analyse.DtoShared.ValidParameter;

import static com.squareup.javapoet.MethodSpec.methodBuilder;
import static com.squareup.javapoet.TypeSpec.interfaceBuilder;
import static com.squareup.javapoet.WildcardTypeName.subtypeOf;
import static javax.lang.model.element.Modifier.ABSTRACT;
import static javax.lang.model.element.Modifier.PUBLIC;
import static net.zerobuilder.compiler.Utilities.iterationVarName;
import static net.zerobuilder.compiler.Utilities.nullCheck;
import static net.zerobuilder.compiler.Utilities.parameterSpec;

public final class StepContext {

  public static abstract class AbstractStep {
    final ClassName thisType;
    final TypeName nextType;
    AbstractStep(ClassName thisType, TypeName nextType) {
      this.thisType = thisType;
      this.nextType = nextType;
    }
    abstract <R> R accept(StepCases<R> cases);
  }

  abstract static class StepCases<R> {
    abstract R regularParameter(RegularStep parameterContext);
    abstract R beansParameter(BeansStep beansParameterContext);
  }

  public final static class RegularStep extends AbstractStep {
    final DtoShared.ValidRegularParameter parameter;
    final ImmutableList<TypeName> declaredExceptions;
    public RegularStep(ClassName thisType, TypeName nextType, DtoShared.ValidRegularParameter parameter, ImmutableList<TypeName> declaredExceptions) {
      super(thisType, nextType);
      this.declaredExceptions = declaredExceptions;
      this.parameter = parameter;
    }
    @Override
    <R> R accept(StepCases<R> cases) {
      return cases.regularParameter(this);
    }
  }

  static final StepCases<ImmutableList<TypeName>> declaredExceptions
      = new StepCases<ImmutableList<TypeName>>() {
    @Override
    ImmutableList<TypeName> regularParameter(RegularStep parameterContext) {
      return parameterContext.declaredExceptions;
    }
    @Override
    ImmutableList<TypeName> beansParameter(BeansStep beansParameterContext) {
      return ImmutableList.of();
    }
  };

  static final StepCases<ValidParameter> validParameter
      = new StepCases<ValidParameter>() {
    @Override
    ValidParameter regularParameter(RegularStep parameterContext) {
      return parameterContext.parameter;
    }
    @Override
    ValidParameter beansParameter(BeansStep beansParameterContext) {
      return beansParameterContext.validBeanParameter;
    }
  };

  public final static class BeansStep extends AbstractStep {
    final ValidBeanParameter validBeanParameter;
    public BeansStep(ClassName thisType, TypeName nextType, ValidBeanParameter validBeanParameter) {
      super(thisType, nextType);
      this.validBeanParameter = validBeanParameter;
    }
    @Override
    <R> R accept(StepCases<R> cases) {
      return cases.beansParameter(this);
    }
  }

  private static <R> StepCases<R> always(final Function<AbstractStep, R> parameterFunction) {
    return new StepCases<R>() {
      @Override
      R regularParameter(RegularStep context) {
        return parameterFunction.apply(context);
      }
      @Override
      R beansParameter(BeansStep context) {
        return parameterFunction.apply(context);
      }
    };
  }

  static final StepCases<CodeBlock> maybeNullCheck
      = always(new Function<AbstractStep, CodeBlock>() {
    @Override
    public CodeBlock apply(AbstractStep context) {
      ValidParameter parameter = context.accept(validParameter);
      if (!parameter.nonNull || parameter.type.isPrimitive()) {
        return CodeBlock.of("");
      }
      return nullCheck(parameter.name, parameter.name);
    }
  });

  static final StepCases<CodeBlock> maybeIterationNullCheck
      = always(new Function<AbstractStep, CodeBlock>() {
    @Override
    public CodeBlock apply(AbstractStep context) {
      ValidParameter parameter = context.accept(validParameter);
      if (!parameter.nonNull || parameter.type.isPrimitive()) {
        return CodeBlock.of("");
      }
      return nullCheck(iterationVarName, parameter.name + " (element)");
    }
  });

  static <R> Function<AbstractStep, R> asFunction(final StepCases<R> cases) {
    return new Function<AbstractStep, R>() {
      @Override
      public R apply(AbstractStep abstractStep) {
        return abstractStep.accept(cases);
      }
    };
  }

  private static final Function<AbstractStep, TypeSpec> regularStepInterface
      = new Function<AbstractStep, TypeSpec>() {
    @Override
    public TypeSpec apply(AbstractStep context) {
      ValidParameter parameter = context.accept(validParameter);
      String name = parameter.name;
      TypeName type = parameter.type;
      return interfaceBuilder(context.thisType)
          .addMethod(methodBuilder(name)
              .returns(context.nextType)
              .addParameter(parameterSpec(type, name))
              .addExceptions(context.accept(declaredExceptions))
              .addModifiers(PUBLIC, ABSTRACT)
              .build())
          .addModifiers(PUBLIC)
          .build();
    }
  };

  private static final Function<BeansStep, TypeSpec> beansStepInterface
      = new Function<BeansStep, TypeSpec>() {
    @Override
    public TypeSpec apply(BeansStep context) {
      ValidBeanParameter parameter = context.validBeanParameter;
      String name = parameter.name;
      if (parameter.collectionType.isPresent()) {
        TypeName collectionType = parameter.collectionType.get();
        ParameterizedTypeName iterable = ParameterizedTypeName.get(ClassName.get(Iterable.class),
            subtypeOf(collectionType));
        TypeSpec.Builder builder = interfaceBuilder(context.thisType)
            .addModifiers(PUBLIC)
            .addMethod(methodBuilder(name)
                .addParameter(parameterSpec(iterable, name))
                .returns(context.nextType)
                .addModifiers(PUBLIC, ABSTRACT)
                .build())
            .addMethod(methodBuilder(name)
                .returns(context.nextType)
                .addModifiers(PUBLIC, ABSTRACT)
                .build());
        if (parameter.collectionType.allowShortcut) {
          builder.addMethod(methodBuilder(name)
              .addParameter(parameterSpec(collectionType, name))
              .returns(context.nextType)
              .addModifiers(PUBLIC, ABSTRACT)
              .build());
        }
        return builder.build();
      } else {
        return regularStepInterface.apply(context);
      }
    }
  };

  static final Function<AbstractStep, TypeSpec> asStepInterface = asFunction(new StepCases<TypeSpec>() {
    @Override
    TypeSpec regularParameter(RegularStep parameterContext) {
      return regularStepInterface.apply(parameterContext);
    }
    @Override
    TypeSpec beansParameter(BeansStep beansParameterContext) {
      return beansStepInterface.apply(beansParameterContext);
    }
  });

  private StepContext() {
    throw new UnsupportedOperationException("no instances");
  }
}
