package net.zerobuilder.compiler.generate;

import com.squareup.javapoet.ClassName;
import com.squareup.javapoet.ParameterSpec;
import com.squareup.javapoet.TypeName;
import net.zerobuilder.compiler.generate.DtoBeanParameter.AbstractBeanParameter;
import net.zerobuilder.compiler.generate.DtoBeanParameter.AccessorPair;
import net.zerobuilder.compiler.generate.DtoBeanParameter.LoneGetter;
import net.zerobuilder.compiler.generate.DtoStep.AbstractStep;
import net.zerobuilder.compiler.generate.DtoStep.CollectionInfo;

import java.util.Optional;
import java.util.function.Function;

import static net.zerobuilder.compiler.generate.Utilities.parameterSpec;
import static net.zerobuilder.compiler.generate.Utilities.upcase;

final class DtoBeanStep {

  interface BeanStepCases<R> {
    R accessorPair(AccessorPairStep step);
    R loneGetter(LoneGetterStep step);
  }

  static <R> Function<AbstractBeanStep, R> asFunction(final BeanStepCases<R> cases) {
    return abstractStep -> abstractStep.acceptBean(cases);
  }

  static <R> Function<AbstractBeanStep, R> beanStepCases(Function<AccessorPairStep, R> accessorPair,
                                            Function<LoneGetterStep, R> loneGetter) {
    return asFunction(new BeanStepCases<R>() {
      @Override
      public R accessorPair(AccessorPairStep step) {
        return accessorPair.apply(step);
      }
      @Override
      public R loneGetter(LoneGetterStep step) {
        return loneGetter.apply(step);
      }
    });
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

    private AccessorPairStep(ClassName thisType, TypeName nextType, AccessorPair accessorPair) {
      super(thisType, nextType);
      this.accessorPair = accessorPair;
    }

    static AccessorPairStep create(ClassName thisType, TypeName nextType, AccessorPair accessorPair) {
      return new AccessorPairStep(thisType, nextType, accessorPair);
    }

    Optional<CollectionInfo> emptyOption() {
      String name = accessorPair.name();
      return CollectionInfo.create(accessorPair.type, name);
    }

    ParameterSpec parameter() {
      return parameterSpec(accessorPair.type, accessorPair.name());
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
      String emptyMethod = "empty" + upcase(loneGetter.name());
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

  static final BeanStepCases<Optional<CollectionInfo>> emptyOption
      = new BeanStepCases<Optional<CollectionInfo>>() {
    @Override
    public Optional<CollectionInfo> accessorPair(AccessorPairStep step) {
      return step.emptyOption();
    }
    @Override
    public Optional<CollectionInfo> loneGetter(LoneGetterStep step) {
      return Optional.empty();
    }
  };

  private DtoBeanStep() {
    throw new UnsupportedOperationException("no instances");
  }
}
