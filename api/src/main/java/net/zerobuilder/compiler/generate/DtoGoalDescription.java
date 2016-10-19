package net.zerobuilder.compiler.generate;

import com.squareup.javapoet.TypeName;
import net.zerobuilder.compiler.generate.DtoBeanGoalDescription.BeanGoalDescription;
import net.zerobuilder.compiler.generate.DtoGoal.AbstractGoalDetails;
import net.zerobuilder.compiler.generate.DtoRegularGoalDescription.RegularGoalDescription;

import java.util.function.Function;

public final class DtoGoalDescription {

  public static abstract class GoalDescription {
    abstract <R> R accept(GoalDescriptionCases<R> cases);
  }

  interface GoalDescriptionCases<R> {
    R regularGoal(RegularGoalDescription goal);
    R beanGoal(BeanGoalDescription goal);
  }

  static <R> Function<GoalDescription, R> asFunction(final GoalDescriptionCases<R> cases) {
    return goal -> goal.accept(cases);
  }

  static <R> Function<GoalDescription, R> goalDescriptionCases(
      Function<? super RegularGoalDescription, ? extends R> regularGoal,
      Function<? super BeanGoalDescription, ? extends R> beanGoal) {
    return asFunction(new GoalDescriptionCases<R>() {
      @Override
      public R regularGoal(RegularGoalDescription goal) {
        return regularGoal.apply(goal);
      }
      @Override
      public R beanGoal(BeanGoalDescription goal) {
        return beanGoal.apply(goal);
      }
    });
  }

  static final Function<GoalDescription, AbstractGoalDetails> goalDetails =
      goalDescriptionCases(
          description -> description.details,
          description -> description.details);

  private static final Function<GoalDescription, AbstractGoalDetails> abstractGoal =
      asFunction(new GoalDescriptionCases<AbstractGoalDetails>() {
        @Override
        public AbstractGoalDetails regularGoal(RegularGoalDescription goal) {
          return goal.details;
        }
        @Override
        public AbstractGoalDetails beanGoal(BeanGoalDescription goal) {
          return goal.details;
        }
      });

  static String goalName(GoalDescription goal) {
    return abstractGoal.apply(goal).name;
  }

  static TypeName goalType(GoalDescription goal) {
    return DtoGoal.goalType.apply(DtoGoalDescription.abstractGoal.apply(goal));
  }

  private DtoGoalDescription() {
    throw new UnsupportedOperationException("no instances");
  }
}
