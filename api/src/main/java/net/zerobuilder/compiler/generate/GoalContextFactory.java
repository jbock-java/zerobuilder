package net.zerobuilder.compiler.generate;

import com.squareup.javapoet.TypeName;
import net.zerobuilder.compiler.generate.DtoBeanGoal.BeanGoalContext;
import net.zerobuilder.compiler.generate.DtoBeanGoalDescription.BeanGoalDescription;
import net.zerobuilder.compiler.generate.DtoBeanParameter.AbstractBeanParameter;
import net.zerobuilder.compiler.generate.DtoBeanParameter.AccessorPair;
import net.zerobuilder.compiler.generate.DtoBeanParameter.BeanParameterCases;
import net.zerobuilder.compiler.generate.DtoBeanParameter.LoneGetter;
import net.zerobuilder.compiler.generate.DtoBeanStep.AbstractBeanStep;
import net.zerobuilder.compiler.generate.DtoBeanStep.AccessorPairStep;
import net.zerobuilder.compiler.generate.DtoBeanStep.LoneGetterStep;
import net.zerobuilder.compiler.generate.DtoConstructorGoal.ConstructorGoalContext;
import net.zerobuilder.compiler.generate.DtoContext.BuildersContext;
import net.zerobuilder.compiler.generate.DtoGoal.AbstractGoalDetails;
import net.zerobuilder.compiler.generate.DtoGoal.ConstructorGoalDetails;
import net.zerobuilder.compiler.generate.DtoGoal.MethodGoalDetails;
import net.zerobuilder.compiler.generate.DtoGoal.RegularGoalCases;
import net.zerobuilder.compiler.generate.DtoGoalContext.AbstractGoalContext;
import net.zerobuilder.compiler.generate.DtoGoalDescription.GoalDescription;
import net.zerobuilder.compiler.generate.DtoMethodGoal.MethodGoalContext;
import net.zerobuilder.compiler.generate.DtoParameter.AbstractParameter;
import net.zerobuilder.compiler.generate.DtoRegularGoal.AbstractRegularGoalContext;
import net.zerobuilder.compiler.generate.DtoRegularGoalDescription.AbstractRegularGoalDescription;
import net.zerobuilder.compiler.generate.DtoRegularParameter.AbstractRegularParameter;
import net.zerobuilder.compiler.generate.DtoRegularParameter.ProjectedParameter;
import net.zerobuilder.compiler.generate.DtoRegularParameter.RegularParameterCases;
import net.zerobuilder.compiler.generate.DtoRegularParameter.SimpleParameter;
import net.zerobuilder.compiler.generate.DtoRegularStep.AbstractRegularStep;
import net.zerobuilder.compiler.generate.DtoRegularStep.ProjectedRegularStep;
import net.zerobuilder.compiler.generate.DtoRegularStep.SimpleRegularStep;
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

  private static BeanGoalContext prepareBean(
      BuildersContext context,
      BeanGoalDescription goal) {
    List<AbstractBeanStep> steps = steps(
        goal,
        context,
        goal.parameters,
        beanFactory);
    return new BeanGoalContext(context, goal.details, steps, goal.thrownTypes);
  }

  private static AbstractRegularGoalContext prepareRegular(
      BuildersContext context,
      AbstractRegularGoalDescription validGoal) {
    List<AbstractRegularStep> steps = steps(
        validGoal,
        context,
        validGoal.parameters(),
        regularFactory);
    return validGoal.details.accept(new RegularGoalCases<AbstractRegularGoalContext>() {
      @Override
      public AbstractRegularGoalContext method(MethodGoalDetails details) {
        return new MethodGoalContext(context, details, steps, validGoal.thrownTypes);
      }
      @Override
      public AbstractRegularGoalContext constructor(ConstructorGoalDetails details) {
        return new ConstructorGoalContext(context, details, steps, validGoal.thrownTypes);
      }
    });
  }

  private static <P extends AbstractParameter, S extends AbstractStep> List<S> steps(
      GoalDescription goal,
      BuildersContext context,
      List<P> parameters,
      Function<P, StepFactory<S>> factoryFactory) {
    AbstractGoalDetails details = goal.details();
    Optional<S> nextStep = Optional.empty();
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

  private static abstract class StepFactory<S extends AbstractStep> {
    abstract S create(String thisType,
                      Optional<S> nextType,
                      AbstractGoalDetails goalDetails,
                      BuildersContext context,
                      List<TypeName> thrownTypes);
  }

  private static final Function<AbstractBeanParameter, StepFactory<AbstractBeanStep>> beanFactory =
      beanParameter -> new StepFactory<AbstractBeanStep>() {
        @Override
        AbstractBeanStep create(String thisType,
                                Optional<AbstractBeanStep> nextType,
                                AbstractGoalDetails goalDetails,
                                BuildersContext context,
                                List<TypeName> declaredExceptions) {
          return beanParameter.accept(new BeanParameterCases<AbstractBeanStep>() {
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

  private static final Function<AbstractRegularParameter, StepFactory<AbstractRegularStep>> regularFactory =
      regularParameter -> new StepFactory<AbstractRegularStep>() {
        @Override
        AbstractRegularStep create(String thisType,
                                   Optional<AbstractRegularStep> nextType,
                                   AbstractGoalDetails goalDetails,
                                   BuildersContext context,
                                   List<TypeName> declaredExceptions) {
          return regularParameter.acceptRegularParameter(new RegularParameterCases<AbstractRegularStep>() {
            @Override
            public AbstractRegularStep simpleParameter(SimpleParameter parameter) {
              return SimpleRegularStep.create(
                  thisType,
                  nextType,
                  goalDetails,
                  context,
                  parameter);
            }
            @Override
            public AbstractRegularStep projectedParameter(ProjectedParameter parameter) {
              return ProjectedRegularStep.create(
                  thisType,
                  nextType,
                  goalDetails,
                  context,
                  parameter,
                  declaredExceptions);
            }
          });
        }
      };

  static Function<GoalDescription, AbstractGoalContext> prepare(BuildersContext context) {
    return goalDescriptionCases(
        goal -> GoalContextFactory.prepareRegular(
            context, goal),
        goal -> GoalContextFactory.prepareBean(
            context, goal));
  }
}
