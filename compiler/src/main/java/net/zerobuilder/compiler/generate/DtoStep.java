package net.zerobuilder.compiler.generate;

import com.google.common.base.Function;
import com.google.common.collect.ImmutableList;
import com.squareup.javapoet.ClassName;
import com.squareup.javapoet.FieldSpec;
import com.squareup.javapoet.ParameterSpec;
import com.squareup.javapoet.TypeName;
import net.zerobuilder.compiler.analyse.DtoShared.ValidBeanParameter;
import net.zerobuilder.compiler.analyse.DtoShared.ValidRegularParameter;

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
    R beanStep(BeanStep step);
  }

  static <R> StepCases<R> stepCases(final Function<? super RegularStep, R> regularFunction,
                                    final Function<? super BeanStep, R> beanFunction) {
    return new StepCases<R>() {
      @Override
      public R regularStep(RegularStep step) {
        return regularFunction.apply(step);
      }
      @Override
      public R beanStep(BeanStep step) {
        return beanFunction.apply(step);
      }
    };
  }

  public static final class RegularStep extends AbstractStep {
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

  public static final class BeanStep extends AbstractStep {
    final ValidBeanParameter validParameter;
    final ParameterSpec parameter;

    /**
     * empty iff {@link #validParameter} {@code .collectionType.isPresent()}
     */
    final String setter;
    public BeanStep(ClassName thisType, TypeName nextType, ValidBeanParameter validParameter,
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

  static <R> StepCases<R> always(final Function<AbstractStep, R> parameterFunction) {
    return new StepCases<R>() {
      @Override
      public R regularStep(RegularStep step) {
        return parameterFunction.apply(step);
      }
      @Override
      public R beanStep(BeanStep step) {
        return parameterFunction.apply(step);
      }
    };
  }

  private DtoStep() {
    throw new UnsupportedOperationException("no instances");
  }
}
