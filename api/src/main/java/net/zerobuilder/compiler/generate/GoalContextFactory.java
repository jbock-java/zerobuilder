package net.zerobuilder.compiler.generate;

import com.squareup.javapoet.TypeName;
import net.zerobuilder.compiler.generate.DtoBeanGoal.BeanGoal;
import net.zerobuilder.compiler.generate.DtoBeanGoalDescription.BeanGoalDescription;
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
import net.zerobuilder.compiler.generate.DtoGoalDescription.GoalDescription;
import net.zerobuilder.compiler.generate.DtoParameter.AbstractParameter;
import net.zerobuilder.compiler.generate.DtoRegularGoal.ConstructorGoal;
import net.zerobuilder.compiler.generate.DtoRegularGoal.MethodGoal;
import net.zerobuilder.compiler.generate.DtoRegularGoalDescription.AbstractRegularGoalDescription;
import net.zerobuilder.compiler.generate.DtoRegularParameter.AbstractRegularParameter;
import net.zerobuilder.compiler.generate.DtoRegularStep.RegularStep;
import net.zerobuilder.compiler.generate.DtoStep.AbstractStep;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.function.Function;

import static java.util.Collections.emptyList;
import static net.zerobuilder.compiler.generate.DtoGoalDescription.goalDescriptionCases;
import static net.zerobuilder.compiler.generate.DtoParameter.parameterName;
import static net.zerobuilder.compiler.generate.Utilities.reverse;
import static net.zerobuilder.compiler.generate.Utilities.upcase;

final class GoalContextFactory {

  private static IGoal beanGoal(
      BuildersContext context,
      BeanGoalDescription goal) {
    List<? extends AbstractBeanStep> steps = steps(
        goal,
        context,
        goal.parameters,
        beanFactory);
    return BeanGoal.create(goal.details, steps, goal.thrownTypes);
  }

  private static IGoal regularGoal(
      BuildersContext context,
      AbstractRegularGoalDescription validGoal) {
    List<RegularStep> steps = steps(
        validGoal,
        context,
        validGoal.parameters(),
        regularFactory);
    return validGoal.details.accept(new RegularGoalCases<IGoal>() {
      @Override
      public IGoal method(MethodGoalDetails goal) {
        return MethodGoal.create(goal, steps, validGoal.thrownTypes);
      }
      @Override
      public IGoal constructor(ConstructorGoalDetails goal) {
        return ConstructorGoal.create(goal, steps, validGoal.thrownTypes);
      }
    });
  }

  private static <P extends AbstractParameter, S extends AbstractStep> List<S> steps(
      GoalDescription goal,
      BuildersContext context,
      List<P> parameters,
      Function<P, StepFactory<S>> factoryFactory) {
    AbstractGoalDetails details = goal.details();
    Optional<? extends AbstractStep> nextStep = Optional.empty();
    List<TypeName> thrownTypes = goal.thrownTypes();
    List<S> builder = new ArrayList<>(parameters.size());
    for (P parameter : reverse(parameters)) {
      String thisType = upcase(parameterName.apply(parameter));
      StepFactory<S> factory = factoryFactory.apply(parameter);
      S step = factory.create(
          thisType,
          nextStep,
          details,
          context,
          thrownTypes);
      builder.add(step);
      thrownTypes = emptyList();
      nextStep = Optional.of(step);
    }
    return reverse(builder);
  }

  private static abstract class StepFactory<R extends AbstractStep> {
    abstract R create(String thisType,
                      Optional<? extends AbstractStep> nextType,
                      AbstractGoalDetails goalDetails,
                      BuildersContext context,
                      List<TypeName> thrownTypes);
  }

  private static final Function<AbstractBeanParameter, StepFactory<AbstractBeanStep>> beanFactory =
      regularParameter -> new StepFactory<AbstractBeanStep>() {
        @Override
        AbstractBeanStep create(String thisType,
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

  private static final Function<AbstractRegularParameter, StepFactory<RegularStep>> regularFactory =
      regularParameter -> new StepFactory<RegularStep>() {
        @Override
        RegularStep create(String thisType,
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

  static Function<GoalDescription, AbstractGoalContext> prepare(BuildersContext context) {
    return goalDescriptionCases(
        goal -> GoalContextFactory.regularGoal(
            context, goal)
            .withContext(context),
        goal -> GoalContextFactory.beanGoal(
            context, goal)
            .withContext(context));
  }
}
