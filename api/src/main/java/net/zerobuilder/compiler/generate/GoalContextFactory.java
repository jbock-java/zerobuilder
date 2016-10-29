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
import net.zerobuilder.compiler.generate.DtoConstructorGoal.SimpleConstructorGoalContext;
import net.zerobuilder.compiler.generate.DtoContext.BuildersContext;
import net.zerobuilder.compiler.generate.DtoDescriptionInput.DescriptionInput;
import net.zerobuilder.compiler.generate.DtoGeneratorInput.AbstractGoalInput;
import net.zerobuilder.compiler.generate.DtoGeneratorInput.GoalInput;
import net.zerobuilder.compiler.generate.DtoGeneratorInput.ProjectedGoalInput;
import net.zerobuilder.compiler.generate.DtoGoalDetails.AbstractGoalDetails;
import net.zerobuilder.compiler.generate.DtoGoalDetails.ConstructorGoalDetails;
import net.zerobuilder.compiler.generate.DtoGoalDetails.MethodGoalDetails;
import net.zerobuilder.compiler.generate.DtoGoalDetails.RegularGoalDetailsCases;
import net.zerobuilder.compiler.generate.DtoMethodGoal.SimpleMethodGoalContext;
import net.zerobuilder.compiler.generate.DtoParameter.AbstractParameter;
import net.zerobuilder.compiler.generate.DtoProjectedGoal.ProjectedGoal;
import net.zerobuilder.compiler.generate.DtoProjectedRegularGoalContext.ProjectedConstructorGoalContext;
import net.zerobuilder.compiler.generate.DtoProjectedRegularGoalContext.ProjectedMethodGoalContext;
import net.zerobuilder.compiler.generate.DtoRegularGoal.SimpleRegularGoalContext;
import net.zerobuilder.compiler.generate.DtoRegularGoalDescription.ProjectedRegularGoalDescription;
import net.zerobuilder.compiler.generate.DtoRegularGoalDescription.SimpleRegularGoalDescription;
import net.zerobuilder.compiler.generate.DtoRegularParameter.ProjectedParameter;
import net.zerobuilder.compiler.generate.DtoRegularParameter.SimpleParameter;
import net.zerobuilder.compiler.generate.DtoRegularStep.ProjectedRegularStep;
import net.zerobuilder.compiler.generate.DtoRegularStep.SimpleRegularStep;
import net.zerobuilder.compiler.generate.DtoSimpleGoal.SimpleGoal;
import net.zerobuilder.compiler.generate.DtoStep.AbstractStep;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.function.Function;

import static java.util.Collections.emptyList;
import static net.zerobuilder.compiler.generate.DtoDescriptionInput.descriptionInputCases;
import static net.zerobuilder.compiler.generate.DtoParameter.parameterName;
import static net.zerobuilder.compiler.generate.DtoProjectedDescription.projectedDescriptionCases;
import static net.zerobuilder.compiler.generate.DtoSimpleDescription.simpleDescriptionCases;
import static net.zerobuilder.compiler.generate.Utilities.reverse;
import static net.zerobuilder.compiler.generate.Utilities.upcase;

final class GoalContextFactory {

  private static BeanGoalContext prepareBean(
      BuildersContext context,
      BeanGoalDescription goal) {
    List<AbstractBeanStep> steps = steps(
        goal.details,
        goal.thrownTypes,
        context,
        goal.parameters,
        beanFactory);
    return new BeanGoalContext(context, goal.details, steps, goal.thrownTypes);
  }

  private static SimpleGoal prepareRegular(
      BuildersContext context,
      SimpleRegularGoalDescription simple) {
    List<SimpleRegularStep> steps = steps(
        simple.details,
        simple.thrownTypes,
        context,
        simple.parameters,
        simpleRegularFactory);
    return simple.details.accept(new RegularGoalDetailsCases<SimpleRegularGoalContext>() {
      @Override
      public SimpleRegularGoalContext method(MethodGoalDetails details) {
        return new SimpleMethodGoalContext(context, details, steps, simple.thrownTypes);
      }
      @Override
      public SimpleRegularGoalContext constructor(ConstructorGoalDetails details) {
        return new SimpleConstructorGoalContext(context, details, steps, simple.thrownTypes);
      }
    });
  }


  private static ProjectedGoal prepareProjectedRegular(
      BuildersContext context,
      ProjectedRegularGoalDescription description) {
    List<ProjectedRegularStep> steps = steps(
        description.details,
        description.thrownTypes,
        context,
        description.parameters,
        projectedRegularFactory);
    return description.details.accept(new RegularGoalDetailsCases<ProjectedGoal>() {
      @Override
      public ProjectedGoal method(MethodGoalDetails details) {
        return new ProjectedMethodGoalContext(context, details, steps, description.thrownTypes);
      }
      @Override
      public ProjectedGoal constructor(ConstructorGoalDetails details) {
        return new ProjectedConstructorGoalContext(context, details, steps, description.thrownTypes);
      }
    });
  }

  private static <P extends AbstractParameter, S extends AbstractStep> List<S> steps(
      AbstractGoalDetails details,
      List<TypeName> thrownTypes,
      BuildersContext context,
      List<P> parameters,
      Function<P, StepFactory<S>> factoryFactory) {
    Optional<S> nextStep = Optional.empty();
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

  private static final Function<SimpleParameter, StepFactory<SimpleRegularStep>> simpleRegularFactory =
      regularParameter -> new StepFactory<SimpleRegularStep>() {
        @Override
        SimpleRegularStep create(String thisType, Optional<SimpleRegularStep> nextType, AbstractGoalDetails goalDetails, BuildersContext context, List<TypeName> thrownTypes) {
          return SimpleRegularStep.create(
              thisType,
              nextType,
              goalDetails,
              context,
              regularParameter);
        }
      };

  private static final Function<ProjectedParameter, StepFactory<ProjectedRegularStep>> projectedRegularFactory =
      regularParameter -> new StepFactory<ProjectedRegularStep>() {
        @Override
        ProjectedRegularStep create(String thisType, Optional<ProjectedRegularStep> nextType, AbstractGoalDetails goalDetails, BuildersContext context, List<TypeName> thrownTypes) {
          return ProjectedRegularStep.create(
              thisType,
              nextType,
              goalDetails,
              context,
              regularParameter,
              thrownTypes);
        }
      };

  static Function<DescriptionInput, AbstractGoalInput> prepare(BuildersContext context) {
    return descriptionInputCases(
        (module, description) -> new GoalInput(
            module,
            simpleDescriptionCases(
                regular -> GoalContextFactory.prepareRegular(
                    context, regular),
                bean -> GoalContextFactory.prepareBean(
                    context, bean)).apply(description)),
        (module, description) -> new ProjectedGoalInput(
            module,
            projectedDescriptionCases(
                regular -> prepareProjectedRegular(
                    context, regular),
                bean -> GoalContextFactory.prepareBean(
                    context, bean)).apply(description)));
  }
}
