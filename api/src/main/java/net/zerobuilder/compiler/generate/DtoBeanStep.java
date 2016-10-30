package net.zerobuilder.compiler.generate;

import com.squareup.javapoet.ParameterSpec;
import com.squareup.javapoet.TypeName;
import net.zerobuilder.compiler.generate.DtoBeanParameter.AbstractBeanParameter;
import net.zerobuilder.compiler.generate.DtoBeanParameter.AccessorPair;
import net.zerobuilder.compiler.generate.DtoBeanParameter.LoneGetter;
import net.zerobuilder.compiler.generate.DtoContext.BuildersContext;
import net.zerobuilder.compiler.generate.DtoGoalDetails.AbstractGoalDetails;
import net.zerobuilder.compiler.generate.DtoStep.AbstractStep;
import net.zerobuilder.compiler.generate.DtoStep.CollectionInfo;

import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.function.Function;

import static net.zerobuilder.compiler.generate.ZeroUtil.parameterSpec;
import static net.zerobuilder.compiler.generate.ZeroUtil.upcase;

public final class DtoBeanStep {

  interface BeanStepCases<R> {
    R accessorPair(AccessorPairStep step);
    R loneGetter(LoneGetterStep step);
  }

  static <R> Function<AbstractBeanStep, R> asFunction(final BeanStepCases<R> cases) {
    return abstractStep -> abstractStep.acceptBean(cases);
  }

  public static <R> Function<AbstractBeanStep, R> beanStepCases(Function<AccessorPairStep, R> accessorPair,
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

  public static abstract class AbstractBeanStep extends AbstractStep {
    AbstractBeanStep(String thisType,
                     Optional<? extends AbstractStep> nextType,
                     AbstractGoalDetails goalDetails,
                     BuildersContext context) {
      super(thisType, nextType, goalDetails, context);
    }
    @Override
    final <R> R accept(DtoStep.StepCases<R> cases) {
      return cases.beanStep(this);
    }
    abstract <R> R acceptBean(BeanStepCases<R> cases);

    public final List<TypeName> getterThrownTypes() {
      return getterThrownTypes.apply(this);
    }

    public final List<TypeName> setterThrownTypes() {
      return setterThrownTypes.apply(this);
    }

    public final AbstractBeanParameter beanParameter() {
      return validBeanParameter.apply(this);
    }

  }

  public static final class AccessorPairStep extends AbstractBeanStep {
    public final AccessorPair accessorPair;

    private AccessorPairStep(String thisType,
                             Optional<? extends AbstractStep> nextType,
                             AbstractGoalDetails goalDetails,
                             BuildersContext context,
                             AccessorPair accessorPair) {
      super(thisType, nextType, goalDetails, context);
      this.accessorPair = accessorPair;
    }

    static AccessorPairStep create(String thisType,
                                   Optional<? extends AbstractStep> nextType,
                                   AbstractGoalDetails goalDetails,
                                   BuildersContext context,
                                   AccessorPair accessorPair) {
      return new AccessorPairStep(thisType, nextType, goalDetails, context, accessorPair);
    }

    public Optional<CollectionInfo> emptyOption() {
      String name = accessorPair.name();
      return CollectionInfo.create(accessorPair.type, name);
    }


    public ParameterSpec parameter() {
      return parameterSpec(accessorPair.type, accessorPair.name());
    }

    @Override
    <R> R acceptBean(BeanStepCases<R> cases) {
      return cases.accessorPair(this);
    }
  }

  public static final class LoneGetterStep extends AbstractBeanStep {
    public final LoneGetter loneGetter;
    public final String emptyMethod;

    private LoneGetterStep(String thisType,
                           Optional<? extends AbstractStep> nextType,
                           AbstractGoalDetails goalDetails,
                           BuildersContext context,
                           LoneGetter loneGetter,
                           String emptyMethod) {
      super(thisType, nextType, goalDetails, context);
      this.loneGetter = loneGetter;
      this.emptyMethod = emptyMethod;
    }

    static LoneGetterStep create(String thisType,
                                 Optional<? extends AbstractStep> nextType,
                                 AbstractGoalDetails goalDetails,
                                 BuildersContext context,
                                 LoneGetter loneGetter) {
      String emptyMethod = "empty" + upcase(loneGetter.name());
      return new LoneGetterStep(thisType, nextType, goalDetails, context, loneGetter, emptyMethod);
    }

    @Override
    <R> R acceptBean(BeanStepCases<R> cases) {
      return cases.loneGetter(this);
    }
  }

  private static final Function<AbstractBeanStep, AbstractBeanParameter> validBeanParameter
      = beanStepCases(
      step -> step.accessorPair,
      step -> step.loneGetter);

  private static final Function<AbstractBeanStep, List<TypeName>> getterThrownTypes
      = beanStepCases(step -> step.accessorPair.getterThrownTypes,
      step -> step.loneGetter.getterThrownTypes);

  private static final Function<AbstractBeanStep, List<TypeName>> setterThrownTypes
      = beanStepCases(step -> step.accessorPair.setterThrownTypes,
      step -> Collections.emptyList());

  private DtoBeanStep() {
    throw new UnsupportedOperationException("no instances");
  }
}
