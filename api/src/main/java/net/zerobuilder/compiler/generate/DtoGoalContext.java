package net.zerobuilder.compiler.generate;

import com.squareup.javapoet.TypeName;
import net.zerobuilder.compiler.generate.DtoBeanGoal.BeanGoalContext;
import net.zerobuilder.compiler.generate.DtoContext.BuildersContext;
import net.zerobuilder.compiler.generate.DtoGoalDetails.AbstractGoalDetails;
import net.zerobuilder.compiler.generate.DtoProjectedRegularGoalContext.ProjectedRegularGoalContext;
import net.zerobuilder.compiler.generate.DtoRegularGoalContext.RegularGoalContext;
import net.zerobuilder.compiler.generate.DtoStep.AbstractStep;

import java.util.List;
import java.util.function.Function;

import static java.util.Collections.unmodifiableList;
import static net.zerobuilder.compiler.generate.DtoProjectedRegularGoalContext.projectedRegularGoalContextCases;
import static net.zerobuilder.compiler.generate.DtoRegularGoalContext.regularGoalContextCases;

final class DtoGoalContext {

  static abstract class AbstractGoalContext {

    abstract <R> R accept(GoalCases<R> cases);

    public final List<? extends AbstractStep> steps() {
      return abstractSteps.apply(this);
    }

    public final String name() {
      return goalName.apply(this);
    }

    final AbstractGoalDetails details() {
      return abstractGoalDetails.apply(this);
    }

    public final BuildersContext context() {
      return context.apply(this);
    }

    final TypeName goalType() {
      return goalType.apply(this);
    }
  }

  interface GoalCases<R> {
    R regularGoal(RegularGoalContext goal);
    R beanGoal(BeanGoalContext goal);
  }

  static <R> Function<AbstractGoalContext, R> asFunction(final GoalCases<R> cases) {
    return goal -> goal.accept(cases);
  }

  static <R> Function<AbstractGoalContext, R> goalCases(
      Function<? super RegularGoalContext, ? extends R> regularFunction,
      Function<? super BeanGoalContext, ? extends R> beanFunction) {
    return asFunction(new GoalCases<R>() {
      @Override
      public R regularGoal(RegularGoalContext goal) {
        return regularFunction.apply(goal);
      }
      @Override
      public R beanGoal(BeanGoalContext goal) {
        return beanFunction.apply(goal);
      }
    });
  }

  static final Function<RegularGoalContext, BuildersContext> regularContext =
      regularGoalContextCases(
          DtoRegularGoal.regularGoalContextCases(
              constructor -> constructor.context,
              method -> method.context,
              staticMethod -> staticMethod.context),
          projectedRegularGoalContextCases(
              method -> method.context,
              constructor -> constructor.context));

  static final Function<AbstractGoalContext, BuildersContext> context =
      goalCases(
          regularContext,
          bean -> bean.context);

  private static final Function<ProjectedRegularGoalContext, AbstractGoalDetails> PROJECTED_REGULAR_GOAL_CONTEXT_R_FUNCTION = projectedRegularGoalContextCases(
      method -> method.details,
      constructor -> constructor.details);

  private static final Function<RegularGoalContext, AbstractGoalDetails> REGULAR_GOAL_CONTEXT_R_FUNCTION =
      regularGoalContextCases(
          DtoRegularGoal.goalDetails,
          PROJECTED_REGULAR_GOAL_CONTEXT_R_FUNCTION);

  private static final Function<AbstractGoalContext, AbstractGoalDetails> abstractGoalDetails =
      goalCases(
          REGULAR_GOAL_CONTEXT_R_FUNCTION,
          bean -> bean.details);

  private static final Function<AbstractGoalContext, TypeName> goalType =
      goalCases(
          regularGoalContextCases(
              DtoRegularGoal.regularGoalContextCases(
                  constructor -> constructor.details.goalType,
                  method -> method.details.goalType,
                  staticMethod -> staticMethod.details.goalType),
              projectedRegularGoalContextCases(
                  method -> method.details.goalType,
                  constructor -> constructor.details.goalType)),
          bean -> bean.details.goalType);

  private static final Function<AbstractGoalContext, String> goalName =
      goalCases(
          regularGoalContextCases(
              DtoRegularGoal.regularGoalContextCases(
                  constructor -> constructor.details.name,
                  method -> method.details.name,
                  staticMethod -> staticMethod.details.name),
              projectedRegularGoalContextCases(
                  method -> method.details.name,
                  constructor -> constructor.details.name)),
          bean -> bean.details.name);

  static final Function<AbstractGoalContext, List<? extends AbstractStep>> abstractSteps =
      goalCases(
          regularGoalContextCases(
              DtoRegularGoal.regularGoalContextCases(
                  constructor -> constructor.steps,
                  method -> method.steps,
                  staticMethod -> staticMethod.steps),
              projectedRegularGoalContextCases(
                  method -> method.steps,
                  constructor -> constructor.steps)),
          bean -> unmodifiableList(bean.steps));

  private DtoGoalContext() {
    throw new UnsupportedOperationException("no instances");
  }
}
