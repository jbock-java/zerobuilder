package net.zerobuilder.compiler.generate;

import com.google.common.base.Function;
import com.google.common.base.Optional;
import com.squareup.javapoet.ClassName;
import com.squareup.javapoet.ParameterSpec;
import com.squareup.javapoet.TypeName;
import net.zerobuilder.compiler.generate.DtoBeanParameter.AbstractBeanParameter;
import net.zerobuilder.compiler.generate.DtoBeanParameter.AccessorPair;
import net.zerobuilder.compiler.generate.DtoBeanParameter.LoneGetter;
import net.zerobuilder.compiler.generate.DtoStep.AbstractStep;
import net.zerobuilder.compiler.generate.DtoStep.EmptyOption;

import static net.zerobuilder.compiler.generate.Utilities.parameterSpec;
import static net.zerobuilder.compiler.generate.Utilities.upcase;
import static net.zerobuilder.compiler.generate.DtoBeanParameter.beanParameterName;

final class DtoBeanStep {

  interface BeanStepCases<R> {
    R accessorPair(AccessorPairStep step);
    R loneGetter(LoneGetterStep step);
  }

  static <R> Function<AbstractBeanStep, R> asFunction(final BeanStepCases<R> cases) {
    return new Function<AbstractBeanStep, R>() {
      @Override
      public R apply(AbstractBeanStep abstractStep) {
        return abstractStep.acceptBean(cases);
      }
    };
  }

  static abstract class AbstractBeanStep extends AbstractStep {
    AbstractBeanStep(ClassName thisType, TypeName nextType) {
      super(thisType, nextType);
    }
    @Override
    final <R> R accept(DtoStep.StepCases<R> cases) {
      return cases.beanStep(this);
    }
    abstract <R> R acceptBean(BeanStepCases<R> cases);
  }

  static final class AccessorPairStep extends AbstractBeanStep {
    final AccessorPair accessorPair;
    final String setter;

    private AccessorPairStep(ClassName thisType, TypeName nextType, AccessorPair accessorPair,
                             String setter) {
      super(thisType, nextType);
      this.accessorPair = accessorPair;
      this.setter = setter;
    }

    static AccessorPairStep create(ClassName thisType, TypeName nextType, AccessorPair accessorPair,
                                          String setter) {
      return new AccessorPairStep(thisType, nextType, accessorPair, setter);
    }

    Optional<EmptyOption> emptyOption() {
      String name = accessorPair.accept(beanParameterName);
      return EmptyOption.create(accessorPair.type, name);
    }

    ParameterSpec parameter() {
      return parameterSpec(accessorPair.type, accessorPair.accept(beanParameterName));
    }

    @Override
    <R> R acceptBean(BeanStepCases<R> cases) {
      return cases.accessorPair(this);
    }
  }

  static final class LoneGetterStep extends AbstractBeanStep {
    final LoneGetter loneGetter;
    final String emptyMethod;

    private LoneGetterStep(ClassName thisType, TypeName nextType, LoneGetter loneGetter, String emptyMethod) {
      super(thisType, nextType);
      this.loneGetter = loneGetter;
      this.emptyMethod = emptyMethod;
    }
    static LoneGetterStep create(ClassName thisType, TypeName nextType, LoneGetter loneGetter) {
      String emptyMethod = "empty" + upcase(loneGetter.accept(beanParameterName));
      return new LoneGetterStep(thisType, nextType, loneGetter, emptyMethod);
    }
    @Override
    <R> R acceptBean(BeanStepCases<R> cases) {
      return cases.loneGetter(this);
    }
  }

  static final BeanStepCases<AbstractBeanParameter> validBeanParameter
      = new BeanStepCases<AbstractBeanParameter>() {
    @Override
    public AbstractBeanParameter accessorPair(AccessorPairStep step) {
      return step.accessorPair;
    }
    @Override
    public AbstractBeanParameter loneGetter(LoneGetterStep step) {
      return step.loneGetter;
    }
  };

  static final BeanStepCases<Optional<EmptyOption>> emptyOption
      = new BeanStepCases<Optional<EmptyOption>>() {
    @Override
    public Optional<EmptyOption> accessorPair(AccessorPairStep step) {
      return step.emptyOption();
    }
    @Override
    public Optional<EmptyOption> loneGetter(LoneGetterStep step) {
      return Optional.absent();
    }
  };

  private DtoBeanStep() {
    throw new UnsupportedOperationException("no instances");
  }
}
