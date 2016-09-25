package net.zerobuilder.compiler.generate;

import com.google.common.base.Function;
import com.google.common.collect.ImmutableList;
import com.squareup.javapoet.ClassName;
import com.squareup.javapoet.CodeBlock;
import com.squareup.javapoet.FieldSpec;
import com.squareup.javapoet.ParameterSpec;
import com.squareup.javapoet.ParameterizedTypeName;
import com.squareup.javapoet.TypeName;
import com.squareup.javapoet.TypeSpec;
import net.zerobuilder.compiler.analyse.DtoShared.ValidBeanParameter;
import net.zerobuilder.compiler.analyse.DtoShared.ValidParameter;
import net.zerobuilder.compiler.analyse.DtoShared.ValidRegularParameter;

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
    abstract R regularStep(RegularStep step);
    abstract R beanStep(BeansStep step);
  }

  public final static class RegularStep extends AbstractStep {
    final ValidRegularParameter validParameter;
    final ImmutableList<TypeName> declaredExceptions;
    final FieldSpec field;
    final ParameterSpec parameter;
    public RegularStep(ClassName thisType, TypeName nextType, ValidRegularParameter validParameter,
                       ImmutableList<TypeName> declaredExceptions, FieldSpec field, ParameterSpec parameter) {
      super(thisType, nextType);
      this.declaredExceptions = declaredExceptions;
      this.validParameter = validParameter;
      this.field = field;
      this.parameter = parameter;
    }
    @Override
    <R> R accept(StepCases<R> cases) {
      return cases.regularStep(this);
    }
  }

  static final StepCases<ImmutableList<TypeName>> declaredExceptions
      = new StepCases<ImmutableList<TypeName>>() {
    @Override
    ImmutableList<TypeName> regularStep(RegularStep step) {
      return step.declaredExceptions;
    }
    @Override
    ImmutableList<TypeName> beanStep(BeansStep step) {
      return ImmutableList.of();
    }
  };

  static final StepCases<ValidParameter> validParameter
      = new StepCases<ValidParameter>() {
    @Override
    ValidParameter regularStep(RegularStep step) {
      return step.validParameter;
    }
    @Override
    ValidParameter beanStep(BeansStep step) {
      return step.validParameter;
    }
  };

  public final static class BeansStep extends AbstractStep {
    final ValidBeanParameter validParameter;
    final ParameterSpec parameter;

    /**
     * empty iff {@link #validParameter} {@code .collectionType.isPresent()}
     */
    final String setter;
    public BeansStep(ClassName thisType, TypeName nextType, ValidBeanParameter validParameter,
                     ParameterSpec parameter, String setter) {
      super(thisType, nextType);
      this.validParameter = validParameter;
      this.parameter = parameter;
      this.setter = setter;
    }
    @Override
    <R> R accept(StepCases<R> cases) {
      return cases.beanStep(this);
    }
  }

  private static <R> StepCases<R> always(final Function<AbstractStep, R> parameterFunction) {
    return new StepCases<R>() {
      @Override
      R regularStep(RegularStep step) {
        return parameterFunction.apply(step);
      }
      @Override
      R beanStep(BeansStep step) {
        return parameterFunction.apply(step);
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
      ValidBeanParameter parameter = context.validParameter;
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
    TypeSpec regularStep(RegularStep step) {
      return regularStepInterface.apply(step);
    }
    @Override
    TypeSpec beanStep(BeansStep step) {
      return beansStepInterface.apply(step);
    }
  });

  private StepContext() {
    throw new UnsupportedOperationException("no instances");
  }
}
