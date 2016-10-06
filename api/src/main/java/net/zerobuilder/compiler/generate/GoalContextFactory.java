package net.zerobuilder.compiler.generate;

import com.google.common.base.Function;
import com.google.common.collect.ImmutableList;
import com.squareup.javapoet.ClassName;
import com.squareup.javapoet.TypeName;
import net.zerobuilder.compiler.generate.DtoBeanParameter.AbstractBeanParameter;
import net.zerobuilder.compiler.generate.DtoBeanParameter.AccessorPair;
import net.zerobuilder.compiler.generate.DtoBeanParameter.LoneGetter;
import net.zerobuilder.compiler.generate.DtoBeanStep.AbstractBeanStep;
import net.zerobuilder.compiler.generate.DtoBeanStep.AccessorPairStep;
import net.zerobuilder.compiler.generate.DtoBeanStep.LoneGetterStep;
import net.zerobuilder.compiler.generate.DtoGoal.ConstructorGoalDetails;
import net.zerobuilder.compiler.generate.DtoGoal.MethodGoalDetails;
import net.zerobuilder.compiler.generate.DtoGoal.RegularGoalCases;
import net.zerobuilder.compiler.generate.DtoGoalContext.IGoal;
import net.zerobuilder.compiler.generate.DtoGoalDescription.BeanGoalDescription;
import net.zerobuilder.compiler.generate.DtoGoalDescription.GoalDescription;
import net.zerobuilder.compiler.generate.DtoGoalDescription.GoalDescriptionCases;
import net.zerobuilder.compiler.generate.DtoGoalDescription.RegularGoalDescription;
import net.zerobuilder.compiler.generate.DtoParameter.AbstractParameter;
import net.zerobuilder.compiler.generate.DtoParameter.RegularParameter;
import net.zerobuilder.compiler.generate.DtoRegularGoalContext.ConstructorGoal;
import net.zerobuilder.compiler.generate.DtoRegularGoalContext.MethodGoal;
import net.zerobuilder.compiler.generate.DtoStep.AbstractStep;
import net.zerobuilder.compiler.generate.DtoStep.RegularStep;

import static net.zerobuilder.compiler.generate.DtoGoalContext.contractName;
import static net.zerobuilder.compiler.generate.DtoGoalDescription.asFunction;
import static net.zerobuilder.compiler.generate.DtoGoalDescription.goalName;
import static net.zerobuilder.compiler.generate.DtoGoalDescription.goalType;
import static net.zerobuilder.compiler.generate.DtoParameter.parameterName;
import static net.zerobuilder.compiler.generate.Utilities.upcase;

final class GoalContextFactory {

  static Function<GoalDescription, IGoal> prepareGoal(final ClassName generatedType) {
    return DtoGoalDescription.asFunction(new GoalDescriptionCases<IGoal>() {
      @Override
      public IGoal regularGoal(RegularGoalDescription goal) {
        return GoalContextFactory.regularGoal(generatedType, goal);
      }
      @Override
      public IGoal beanGoal(BeanGoalDescription goal) {
        return GoalContextFactory.beanGoal(goal, generatedType);
      }
    });
  }

  private static IGoal beanGoal(BeanGoalDescription goal, ClassName generatedType) {
    ImmutableList<? extends AbstractBeanStep> steps = steps(goal,
        generatedType,
        goal.parameters,
        beansParameterFactory);
    return DtoBeanGoalContext.BeanGoal.create(goal.details, steps);
  }

  private static IGoal regularGoal(ClassName generatedType,
                                   final RegularGoalDescription validGoal) {
    final ImmutableList<RegularStep> steps = steps(validGoal,
        generatedType,
        validGoal.parameters,
        regularParameterFactory);
    return DtoGoal.asFunction(new RegularGoalCases<IGoal>() {
      @Override
      public IGoal method(MethodGoalDetails goal) {
        return MethodGoal.create(goal, steps, validGoal.thrownTypes);
      }
      @Override
      public IGoal constructor(ConstructorGoalDetails goal) {
        return ConstructorGoal.create(goal, steps, validGoal.thrownTypes);
      }
    }).apply(validGoal.details);
  }

  private static <P extends AbstractParameter, S extends AbstractStep>
  ImmutableList<S> steps(GoalDescription goal,
                         ClassName generatedType,
                         ImmutableList<P> parameters,
                         ParameterFactory<P, S> parameterFactory) {
    ClassName contractName = contractName(goalName(goal), generatedType);
    TypeName nextType = goalType(goal);
    ImmutableList<TypeName> thrownTypes = GoalContextFactory.thrownTypes.apply(goal);
    ImmutableList.Builder<S> builder = ImmutableList.builder();
    for (P parameter : parameters.reverse()) {
      String thisName = upcase(parameterName.apply(parameter));
      ClassName thisType = contractName.nestedClass(thisName);
      builder.add(parameterFactory.create(thisType, nextType, parameter, thrownTypes));
      nextType = thisType;
    }
    return builder.build().reverse();
  }

  private static abstract class ParameterFactory<P extends AbstractParameter, R extends AbstractStep> {
    abstract R create(ClassName typeThisStep, TypeName typeNextStep, P parameter, ImmutableList<TypeName> declaredExceptions);
  }

  private static final ParameterFactory<AbstractBeanParameter, ? extends AbstractBeanStep> beansParameterFactory
      = new ParameterFactory<AbstractBeanParameter, AbstractBeanStep>() {
    @Override
    AbstractBeanStep create(final ClassName thisType, final TypeName nextType, final AbstractBeanParameter validParameter, ImmutableList<TypeName> declaredExceptions) {
      return validParameter.accept(new DtoBeanParameter.BeanParameterCases<AbstractBeanStep>() {
        @Override
        public AbstractBeanStep accessorPair(AccessorPair pair) {
          String setter = "set" + upcase(parameterName.apply(pair));
          return AccessorPairStep.create(thisType, nextType, pair, setter);
        }
        @Override
        public AbstractBeanStep loneGetter(LoneGetter loneGetter) {
          return LoneGetterStep.create(thisType, nextType, loneGetter);
        }
      });
    }
  };

  private static final ParameterFactory<RegularParameter, RegularStep> regularParameterFactory
      = new ParameterFactory<RegularParameter, RegularStep>() {
    @Override
    RegularStep create(ClassName thisType, TypeName nextType, RegularParameter validParameter, ImmutableList<TypeName> declaredExceptions) {
      return RegularStep.create(thisType, nextType, validParameter, declaredExceptions);
    }
  };

  private static final Function<GoalDescription, ImmutableList<TypeName>> thrownTypes
      = asFunction(new GoalDescriptionCases<ImmutableList<TypeName>>() {
    @Override
    public ImmutableList<TypeName> regularGoal(RegularGoalDescription goal) {
      return goal.thrownTypes;
    }
    @Override
    public ImmutableList<TypeName> beanGoal(BeanGoalDescription goal) {
      return ImmutableList.of();
    }
  });

}
