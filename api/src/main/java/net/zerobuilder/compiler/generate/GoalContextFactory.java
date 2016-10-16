package net.zerobuilder.compiler.generate;

import com.squareup.javapoet.ClassName;
import com.squareup.javapoet.TypeName;
import net.zerobuilder.compiler.generate.DtoBeanParameter.AbstractBeanParameter;
import net.zerobuilder.compiler.generate.DtoBeanParameter.AccessorPair;
import net.zerobuilder.compiler.generate.DtoBeanParameter.BeanParameterCases;
import net.zerobuilder.compiler.generate.DtoBeanParameter.LoneGetter;
import net.zerobuilder.compiler.generate.DtoBeanStep.AbstractBeanStep;
import net.zerobuilder.compiler.generate.DtoBeanStep.AccessorPairStep;
import net.zerobuilder.compiler.generate.DtoBeanStep.LoneGetterStep;
import net.zerobuilder.compiler.generate.DtoContext.BuildersContext;
import net.zerobuilder.compiler.generate.DtoGoal.AbstractGoalDetails;
import net.zerobuilder.compiler.generate.DtoGoal.ConstructorGoalDetails;
import net.zerobuilder.compiler.generate.DtoGoal.MethodGoalDetails;
import net.zerobuilder.compiler.generate.DtoGoal.RegularGoalCases;
import net.zerobuilder.compiler.generate.DtoGoalContext.AbstractGoalContext;
import net.zerobuilder.compiler.generate.DtoGoalContext.IGoal;
import net.zerobuilder.compiler.generate.DtoGoalDescription.BeanGoalDescription;
import net.zerobuilder.compiler.generate.DtoGoalDescription.GoalDescription;
import net.zerobuilder.compiler.generate.DtoGoalDescription.GoalDescriptionCases;
import net.zerobuilder.compiler.generate.DtoGoalDescription.RegularGoalDescription;
import net.zerobuilder.compiler.generate.DtoParameter.AbstractParameter;
import net.zerobuilder.compiler.generate.DtoParameter.RegularParameter;
import net.zerobuilder.compiler.generate.DtoRegularGoal.ConstructorGoal;
import net.zerobuilder.compiler.generate.DtoRegularGoal.MethodGoal;
import net.zerobuilder.compiler.generate.DtoStep.AbstractStep;
import net.zerobuilder.compiler.generate.DtoStep.RegularStep;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.function.Function;

import static net.zerobuilder.compiler.generate.DtoGoalContext.contractName;
import static net.zerobuilder.compiler.generate.DtoGoalDescription.asFunction;
import static net.zerobuilder.compiler.generate.DtoGoalDescription.goalDetails;
import static net.zerobuilder.compiler.generate.DtoGoalDescription.goalName;
import static net.zerobuilder.compiler.generate.DtoParameter.parameterName;
import static net.zerobuilder.compiler.generate.Utilities.reverse;
import static net.zerobuilder.compiler.generate.Utilities.upcase;

final class GoalContextFactory {

  private static IGoal beanGoal(BuildersContext context,
                                BeanGoalDescription goal, ClassName generatedType) {
    List<? extends AbstractBeanStep> steps = steps(
        goal,
        context,
        generatedType,
        goal.parameters,
        beanFactory);
    return DtoBeanGoal.BeanGoal.create(goal.details, steps, goal.thrownTypes);
  }

  private static IGoal regularGoal(BuildersContext context,
                                   ClassName generatedType,
                                   final RegularGoalDescription validGoal) {
    final List<RegularStep> steps = steps(
        validGoal,
        context,
        generatedType,
        validGoal.parameters,
        regularFactory);
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
  List<S> steps(GoalDescription goal,
                BuildersContext context,
                ClassName generatedType,
                List<P> parameters,
                Function<P, StepFactory<S>> factoryFactory) {
    ClassName contractName = contractName(goalName(goal), generatedType);
    AbstractGoalDetails details = goalDetails.apply(goal);
    Optional<? extends AbstractStep> nextStep = Optional.empty();
    List<TypeName> thrownTypes = GoalContextFactory.thrownTypes.apply(goal);
    List<S> builder = new ArrayList<>(parameters.size());
    for (P parameter : reverse(parameters)) {
      String thisName = upcase(parameterName.apply(parameter));
      ClassName thisType = contractName.nestedClass(thisName);
      StepFactory<S> factory = factoryFactory.apply(parameter);
      S step = factory.create(
          thisType,
          nextStep,
          details,
          context,
          thrownTypes);
      builder.add(step);
      nextStep = Optional.of(step);
    }
    return reverse(builder);
  }

  private static abstract class StepFactory<R extends AbstractStep> {
    abstract R create(ClassName thisType,
                      Optional<? extends AbstractStep> nextType,
                      AbstractGoalDetails goalDetails,
                      BuildersContext context,
                      List<TypeName> thrownTypes);
  }

  private static final Function<AbstractBeanParameter, StepFactory<AbstractBeanStep>> beanFactory
      = regularParameter -> new StepFactory<AbstractBeanStep>() {
    @Override
    AbstractBeanStep create(ClassName thisType,
                            Optional<? extends AbstractStep> nextType,
                            AbstractGoalDetails goalDetails,
                            BuildersContext context,
                            List<TypeName> declaredExceptions) {
      return regularParameter.accept(new BeanParameterCases<AbstractBeanStep>() {
        @Override
        public AbstractBeanStep accessorPair(AccessorPair pair) {
          return AccessorPairStep.create(
              thisType,
              nextType,
              goalDetails,
              context,
              pair);
        }
        @Override
        public AbstractBeanStep loneGetter(LoneGetter loneGetter) {
          return LoneGetterStep.create(
              thisType,
              nextType,
              goalDetails,
              context,
              loneGetter);
        }
      });
    }
  };

  private static final Function<RegularParameter, StepFactory<RegularStep>> regularFactory
      = regularParameter -> new StepFactory<RegularStep>() {
    @Override
    RegularStep create(ClassName thisType,
                       Optional<? extends AbstractStep> nextType,
                       AbstractGoalDetails goalDetails,
                       BuildersContext context,
                       List<TypeName> declaredExceptions) {
      return RegularStep.create(
          thisType,
          nextType,
          goalDetails,
          context,
          regularParameter,
          declaredExceptions);
    }
  };

  static Function<GoalDescription, AbstractGoalContext> prepare(BuildersContext context, GeneratorInput goals) {
    ClassName generatedType = goals.buildersContext.generatedType;
    return DtoGoalDescription.asFunction(new GoalDescriptionCases<AbstractGoalContext>() {
      @Override
      public AbstractGoalContext regularGoal(RegularGoalDescription goal) {
        return GoalContextFactory.regularGoal(context, generatedType, goal)
            .withContext(context);
      }
      @Override
      public AbstractGoalContext beanGoal(BeanGoalDescription goal) {
        return GoalContextFactory.beanGoal(context, goal, generatedType)
            .withContext(context);
      }
    });
  }

  private static final Function<GoalDescription, List<TypeName>> thrownTypes
      = asFunction(new GoalDescriptionCases<List<TypeName>>() {
    @Override
    public List<TypeName> regularGoal(RegularGoalDescription goal) {
      return goal.thrownTypes;
    }
    @Override
    public List<TypeName> beanGoal(BeanGoalDescription goal) {
      return Collections.emptyList();
    }
  });
}
