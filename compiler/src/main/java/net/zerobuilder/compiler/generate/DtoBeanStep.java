package net.zerobuilder.compiler.generate;

import com.squareup.javapoet.ClassName;
import com.squareup.javapoet.ParameterSpec;
import com.squareup.javapoet.TypeName;
import net.zerobuilder.compiler.analyse.DtoBeanParameter.AccessorPair;
import net.zerobuilder.compiler.analyse.DtoBeanParameter.LoneGetter;
import net.zerobuilder.compiler.analyse.DtoBeanParameter.ValidBeanParameter;
import net.zerobuilder.compiler.generate.DtoStep.AbstractStep;

import static net.zerobuilder.compiler.Utilities.parameterSpec;
import static net.zerobuilder.compiler.Utilities.upcase;
import static net.zerobuilder.compiler.analyse.DtoBeanParameter.beanParameterName;

public final class DtoBeanStep {

  interface BeanStepCases<R> {
    R accessorPair(AccessorPairStep step);
    R loneGetter(LoneGetterStep step);
  }

  public static abstract class AbstractBeanStep extends AbstractStep {
    AbstractBeanStep(ClassName thisType, TypeName nextType) {
      super(thisType, nextType);
    }
    @Override
    final <R> R accept(DtoStep.StepCases<R> cases) {
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
      return parameterSpec(accessorPair.type, accessorPair.accept(beanParameterName));
    }

    @Override
    <R> R acceptBean(BeanStepCases<R> cases) {
      return cases.accessorPair(this);
    }
  }

  public static final class LoneGetterStep extends AbstractBeanStep {
    final LoneGetter loneGetter;
    final String emptyMethod;

    private LoneGetterStep(ClassName thisType, TypeName nextType, LoneGetter loneGetter, String emptyMethod) {
      super(thisType, nextType);
      this.loneGetter = loneGetter;
      this.emptyMethod = emptyMethod;
    }
    public static LoneGetterStep create(ClassName thisType, TypeName nextType, LoneGetter loneGetter) {
      String emptyMethod = "empty" + upcase(loneGetter.accept(beanParameterName));
      return new LoneGetterStep(thisType, nextType, loneGetter, emptyMethod);
    }
    @Override
    <R> R acceptBean(BeanStepCases<R> cases) {
      return cases.loneGetter(this);
    }
  }

  static final BeanStepCases<ValidBeanParameter> validBeanParameter
      = new BeanStepCases<ValidBeanParameter>() {
    @Override
    public ValidBeanParameter accessorPair(AccessorPairStep step) {
      return step.accessorPair;
    }
    @Override
    public ValidBeanParameter loneGetter(LoneGetterStep step) {
      return step.loneGetter;
    }
  };

  private DtoBeanStep() {
    throw new UnsupportedOperationException("no instances");
  }
}
