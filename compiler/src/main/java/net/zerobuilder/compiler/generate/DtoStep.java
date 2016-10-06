package net.zerobuilder.compiler.generate;

import com.google.common.base.Function;
import com.google.common.base.Optional;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableSet;
import com.squareup.javapoet.ClassName;
import com.squareup.javapoet.CodeBlock;
import com.squareup.javapoet.FieldSpec;
import com.squareup.javapoet.TypeName;
import net.zerobuilder.compiler.Utilities;
import net.zerobuilder.compiler.Utilities.ClassNames;
import net.zerobuilder.compiler.generate.DtoBeanStep.AbstractBeanStep;
import net.zerobuilder.compiler.generate.DtoParameter.AbstractParameter;
import net.zerobuilder.compiler.generate.DtoParameter.RegularParameter;

import java.util.Collections;

import static javax.lang.model.element.Modifier.PRIVATE;
import static net.zerobuilder.compiler.Utilities.ClassNames.COLLECTION;
import static net.zerobuilder.compiler.Utilities.ClassNames.ITERABLE;
import static net.zerobuilder.compiler.Utilities.ClassNames.SET;
import static net.zerobuilder.compiler.Utilities.fieldSpec;
import static net.zerobuilder.compiler.Utilities.rawClassName;
import static net.zerobuilder.compiler.Utilities.upcase;
import static net.zerobuilder.compiler.generate.DtoBeanStep.validBeanParameter;

public final class DtoStep {

  private static final ImmutableSet<ClassName> LIST_HIERARCHY
      = ImmutableSet.of(ClassNames.LIST, COLLECTION, ITERABLE);

  static final class EmptyOption {

    /**
     * Initializer for a variable of type {@link AbstractParameter#type}.
     * It evaluates to an empty List or Set.
     */
    final CodeBlock initializer;

    /**
     * Name of the convenience method to be generated, e.g. {@code "emptyFoo"}
     */
    final String name;

    private EmptyOption(CodeBlock initializer, String name) {
      this.initializer = initializer;
      this.name = name;
    }

    static Optional<EmptyOption> create(TypeName type, String name) {
      Optional<ClassName> maybeClassName = rawClassName(type);
      if (!maybeClassName.isPresent()) {
        return Optional.absent();
      }
      ClassName className = maybeClassName.get();
      if (LIST_HIERARCHY.contains(className)) {
        return Optional.of(new EmptyOption(
            CodeBlock.of("$T.emptyList()", Collections.class),
            emptyOptionName(name)));
      }
      if (SET.equals(className)) {
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

  static <R> Function<AbstractStep, R> asFunction(final StepCases<R> cases) {
    return new Function<AbstractStep, R>() {
      @Override
      public R apply(AbstractStep abstractStep) {
        return abstractStep.accept(cases);
      }
    };
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

    private RegularStep(ClassName thisType, TypeName nextType, RegularParameter validParameter,
                        ImmutableList<TypeName> declaredExceptions) {
      super(thisType, nextType);
      this.declaredExceptions = declaredExceptions;
      this.validParameter = validParameter;
    }

    public static RegularStep create(ClassName thisType, TypeName nextType, RegularParameter parameter,
                                     ImmutableList<TypeName> declaredExceptions) {
      return new RegularStep(thisType, nextType, parameter, declaredExceptions);
    }

    Optional<EmptyOption> emptyOption() {
      return EmptyOption.create(validParameter.type, validParameter.name);
    }

    FieldSpec field() {
      return fieldSpec(validParameter.type, validParameter.name, PRIVATE);
    }

    @Override
    <R> R accept(StepCases<R> cases) {
      return cases.regularStep(this);
    }
  }

  static final Function<AbstractStep, AbstractParameter> abstractParameter
      = asFunction(new StepCases<AbstractParameter>() {
    @Override
    public AbstractParameter regularStep(RegularStep step) {
      return step.validParameter;
    }
    @Override
    public AbstractParameter beanStep(AbstractBeanStep step) {
      return step.acceptBean(validBeanParameter);
    }
  });

  static final Function<AbstractStep, ImmutableList<TypeName>> declaredExceptions
      = asFunction(new StepCases<ImmutableList<TypeName>>() {
    @Override
    public ImmutableList<TypeName> regularStep(RegularStep step) {
      return step.declaredExceptions;
    }
    @Override
    public ImmutableList<TypeName> beanStep(AbstractBeanStep step) {
      return ImmutableList.of();
    }
  });

  static <R> Function<AbstractStep, R> always(final Function<AbstractStep, R> parameterFunction) {
    return asFunction(new StepCases<R>() {
      @Override
      public R regularStep(RegularStep step) {
        return parameterFunction.apply(step);
      }
      @Override
      public R beanStep(AbstractBeanStep step) {
        return parameterFunction.apply(step);
      }
    });
  }

  static final Function<AbstractStep, Optional<EmptyOption>> emptyOption
      = asFunction(new StepCases<Optional<EmptyOption>>() {
    @Override
    public Optional<EmptyOption> regularStep(RegularStep step) {
      return step.emptyOption();
    }
    @Override
    public Optional<EmptyOption> beanStep(AbstractBeanStep step) {
      return step.acceptBean(DtoBeanStep.emptyOption);
    }
  });

  private DtoStep() {
    throw new UnsupportedOperationException("no instances");
  }
}
