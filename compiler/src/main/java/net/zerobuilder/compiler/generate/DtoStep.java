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
import net.zerobuilder.compiler.analyse.DtoValidParameter.ValidRegularParameter;

import static net.zerobuilder.compiler.Utilities.parameterSpec;
import static net.zerobuilder.compiler.analyse.DtoBeanParameter.beanStepName;

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

  interface BeanStepCases<R> {
    R accessorPair(AccessorPairStep step);
    R loneGetter(LoneGetterStep step);
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

  public static abstract class AbstractBeanStep extends AbstractStep {
    AbstractBeanStep(ClassName thisType, TypeName nextType) {
      super(thisType, nextType);
    }
    @Override
    final <R> R accept(StepCases<R> cases) {
      return cases.beanStep(this);
    }
    abstract <R> R acceptBean(BeanStepCases<R> cases);
  }

  public static final class AccessorPairStep extends AbstractBeanStep {
    final AccessorPair accessorPair;
    final String setter;


    public AccessorPairStep(ClassName thisType, TypeName nextType, AccessorPair accessorPair,
                            String setter) {
      super(thisType, nextType);
      this.accessorPair = accessorPair;
      this.setter = setter;
    }

    ParameterSpec parameter() {
      return parameterSpec(accessorPair.type,
          accessorPair.accept(beanStepName));
    }

    @Override
    <R> R acceptBean(BeanStepCases<R> cases) {
      return cases.accessorPair(this);
    }
  }

  public static final class LoneGetterStep extends AbstractBeanStep {
    final LoneGetter loneGetter;

    public LoneGetterStep(ClassName thisType, TypeName nextType, LoneGetter loneGetter) {
      super(thisType, nextType);
      this.loneGetter = loneGetter;
    }
    @Override
    <R> R acceptBean(BeanStepCases<R> cases) {
      return cases.loneGetter(this);
    }
  }

  static final StepCases<DtoValidParameter.ValidParameter> validParameter
      = new StepCases<DtoValidParameter.ValidParameter>() {
    @Override
    public DtoValidParameter.ValidParameter regularStep(RegularStep step) {
      return step.validParameter;
    }
    @Override
    public DtoValidParameter.ValidParameter beanStep(AbstractBeanStep step) {
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

  static final BeanStepCases<DtoBeanParameter.ValidBeanParameter> validBeanParameter
      = new BeanStepCases<DtoBeanParameter.ValidBeanParameter>() {
    @Override
    public DtoBeanParameter.ValidBeanParameter accessorPair(AccessorPairStep step) {
      return step.accessorPair;
    }
    @Override
    public DtoBeanParameter.ValidBeanParameter loneGetter(LoneGetterStep step) {
      return step.loneGetter;
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
