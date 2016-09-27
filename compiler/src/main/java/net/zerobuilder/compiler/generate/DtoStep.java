package net.zerobuilder.compiler.generate;

import com.google.common.base.Function;
import com.google.common.collect.ImmutableList;
import com.squareup.javapoet.ClassName;
import com.squareup.javapoet.FieldSpec;
import com.squareup.javapoet.ParameterSpec;
import com.squareup.javapoet.TypeName;
import net.zerobuilder.compiler.analyse.DtoBeanParameter;
import net.zerobuilder.compiler.analyse.DtoBeanParameter.AccessorPair;
import net.zerobuilder.compiler.analyse.DtoBeanParameter.LoneGetter;
import net.zerobuilder.compiler.analyse.DtoValidParameter;
import net.zerobuilder.compiler.analyse.DtoValidParameter.ValidParameter;
import net.zerobuilder.compiler.analyse.DtoValidParameter.ValidRegularParameter;
import net.zerobuilder.compiler.generate.DtoBeanStep.AbstractBeanStep;

import static net.zerobuilder.compiler.Utilities.parameterSpec;
import static net.zerobuilder.compiler.Utilities.upcase;
import static net.zerobuilder.compiler.analyse.DtoBeanParameter.beanParameterName;
import static net.zerobuilder.compiler.generate.DtoBeanStep.validBeanParameter;

public final class DtoStep {

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
    final ValidRegularParameter validParameter;
    final ImmutableList<TypeName> declaredExceptions;
    final FieldSpec field;

    public RegularStep(ClassName thisType, TypeName nextType, ValidRegularParameter validParameter,
                       ImmutableList<TypeName> declaredExceptions, FieldSpec field) {
      super(thisType, nextType);
      this.declaredExceptions = declaredExceptions;
      this.validParameter = validParameter;
      this.field = field;
    }

    @Override
    <R> R accept(StepCases<R> cases) {
      return cases.regularStep(this);
    }
  }

  static final StepCases<ValidParameter> validParameter
      = new StepCases<ValidParameter>() {
    @Override
    public ValidParameter regularStep(RegularStep step) {
      return step.validParameter;
    }
    @Override
    public ValidParameter beanStep(AbstractBeanStep step) {
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
