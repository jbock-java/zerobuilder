package net.zerobuilder.compiler.generate;

import com.google.common.base.Function;
import com.google.common.base.Optional;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableSet;
import com.squareup.javapoet.ClassName;
import com.squareup.javapoet.CodeBlock;
import com.squareup.javapoet.FieldSpec;
import com.squareup.javapoet.TypeName;
import net.zerobuilder.compiler.Utilities.ClassNames;
import net.zerobuilder.compiler.analyse.DtoParameter.AbstractParameter;
import net.zerobuilder.compiler.analyse.DtoParameter.RegularParameter;
import net.zerobuilder.compiler.generate.DtoBeanStep.AbstractBeanStep;

import java.util.Collections;

import static javax.lang.model.element.Modifier.PRIVATE;
import static net.zerobuilder.compiler.Utilities.ClassNames.COLLECTION;
import static net.zerobuilder.compiler.Utilities.ClassNames.ITERABLE;
import static net.zerobuilder.compiler.Utilities.ClassNames.SET;
import static net.zerobuilder.compiler.Utilities.fieldSpec;
import static net.zerobuilder.compiler.Utilities.upcase;
import static net.zerobuilder.compiler.generate.DtoBeanStep.validBeanParameter;

public final class DtoStep {

  private static final ImmutableSet<ClassName> LIST_HIERARCHY
      = ImmutableSet.of(ClassNames.LIST, COLLECTION, ITERABLE);

  static final class EmptyOption {
    final CodeBlock initializer;
    final String name;

    private EmptyOption(CodeBlock initializer, String name) {
      this.initializer = initializer;
      this.name = name;
    }

    static Optional<EmptyOption> create(TypeName type, String name) {
      if (LIST_HIERARCHY.contains(type)) {
        return Optional.of(new EmptyOption(
            CodeBlock.of("$T.emptyList()", Collections.class),
            emptyOptionName(name)));
      }
      if (SET.equals(type)) {
        return Optional.of(new EmptyOption(
            CodeBlock.of("$T.emptySet()", Collections.class),
            emptyOptionName(name)));
      }
      return Optional.absent();
    }

    private static String emptyOptionName(String name) {
      return "empty" + upcase(name);
    }
  }

  public static abstract class AbstractStep {
    final ClassName thisType;
    final TypeName nextType;
    AbstractStep(ClassName thisType, TypeName nextType) {
      this.thisType = thisType;
      this.nextType = nextType;
    }
    abstract <R> R accept(StepCases<R> cases);
  }

  interface StepCases<R> {
    R regularStep(RegularStep step);
    R beanStep(AbstractBeanStep step);
  }

  static <R> StepCases<R> stepCases(final Function<? super RegularStep, R> regularFunction,
                                    final Function<? super AbstractBeanStep, R> beanFunction) {
    return new StepCases<R>() {
      @Override
      public R regularStep(RegularStep step) {
        return regularFunction.apply(step);
      }
      @Override
      public R beanStep(AbstractBeanStep step) {
        return beanFunction.apply(step);
      }
    };
  }

  public static final class RegularStep extends AbstractStep {
    final RegularParameter validParameter;
    final ImmutableList<TypeName> declaredExceptions;
    final FieldSpec field;
    final Optional<EmptyOption> emptyOption;

    private RegularStep(ClassName thisType, TypeName nextType, RegularParameter validParameter,
                        ImmutableList<TypeName> declaredExceptions, FieldSpec field, Optional<EmptyOption> emptyOption) {
      super(thisType, nextType);
      this.declaredExceptions = declaredExceptions;
      this.validParameter = validParameter;
      this.field = field;
      this.emptyOption = emptyOption;
    }

    public static RegularStep create(ClassName thisType, TypeName nextType, RegularParameter parameter,
                                     ImmutableList<TypeName> declaredExceptions) {
      FieldSpec field = fieldSpec(parameter.type, parameter.name, PRIVATE);
      return new RegularStep(thisType, nextType, parameter, declaredExceptions, field,
          EmptyOption.create(parameter.type, parameter.name));
    }

    @Override
    <R> R accept(StepCases<R> cases) {
      return cases.regularStep(this);
    }
  }

  static final StepCases<AbstractParameter> validParameter
      = new StepCases<AbstractParameter>() {
    @Override
    public AbstractParameter regularStep(RegularStep step) {
      return step.validParameter;
    }
    @Override
    public AbstractParameter beanStep(AbstractBeanStep step) {
      return step.acceptBean(validBeanParameter);
    }
  };

  static final StepCases<ImmutableList<TypeName>> declaredExceptions
      = new StepCases<ImmutableList<TypeName>>() {
    @Override
    public ImmutableList<TypeName> regularStep(RegularStep step) {
      return step.declaredExceptions;
    }
    @Override
    public ImmutableList<TypeName> beanStep(AbstractBeanStep step) {
      return ImmutableList.of();
    }
  };

  static <R> StepCases<R> always(final Function<AbstractStep, R> parameterFunction) {
    return new StepCases<R>() {
      @Override
      public R regularStep(RegularStep step) {
        return parameterFunction.apply(step);
      }
      @Override
      public R beanStep(AbstractBeanStep step) {
        return parameterFunction.apply(step);
      }
    };
  }

  static final StepCases<Optional<EmptyOption>> emptyOption
      = new StepCases<Optional<EmptyOption>>() {
    @Override
    public Optional<EmptyOption> regularStep(RegularStep step) {
      return step.emptyOption;
    }
    @Override
    public Optional<EmptyOption> beanStep(AbstractBeanStep step) {
      return step.acceptBean(DtoBeanStep.emptyOption);
    }
  };

  static <R> Function<AbstractStep, R> asFunction(final StepCases<R> cases) {
    return new Function<AbstractStep, R>() {
      @Override
      public R apply(AbstractStep abstractStep) {
        return abstractStep.accept(cases);
      }
    };
  }

  private DtoStep() {
    throw new UnsupportedOperationException("no instances");
  }
}
