package net.zerobuilder.compiler.analyse;

import com.google.common.base.Function;
import com.google.common.collect.ImmutableList;
import com.squareup.javapoet.TypeName;
import net.zerobuilder.compiler.analyse.DtoGoal.AbstractGoal;
import net.zerobuilder.compiler.analyse.DtoGoalElement.BeanGoalElement;
import net.zerobuilder.compiler.analyse.DtoGoalElement.RegularGoalElement;
import net.zerobuilder.compiler.analyse.DtoParameter.RegularParameter;

final class DtoValidGoal {

  static abstract class ValidGoal {
    abstract <R> R accept(ValidGoalCases<R> cases);
  }

  interface ValidGoalCases<R> {
    R regularGoal(ValidRegularGoal goal);
    R beanGoal(ValidBeanGoal goal);
  }

  private static <R> Function<ValidGoal, R> asFunction(final ValidGoalCases<R> cases) {
    return new Function<ValidGoal, R>() {
      @Override
      public R apply(ValidGoal goal) {
        return goal.accept(cases);
      }
    };
  }

  static final class ValidRegularGoal extends ValidGoal {
    final RegularGoalElement goal;
    final ImmutableList<RegularParameter> parameters;
    ValidRegularGoal(RegularGoalElement goal, ImmutableList<RegularParameter> parameters) {
      this.goal = goal;
      this.parameters = parameters;
    }
    @Override
    final <R> R accept(ValidGoalCases<R> cases) {
      return cases.regularGoal(this);
    }
  }

  static final class ValidBeanGoal extends ValidGoal {
    final BeanGoalElement goal;
    final ImmutableList<DtoBeanParameter.AbstractBeanParameter> parameters;
    ValidBeanGoal(BeanGoalElement goal, ImmutableList<DtoBeanParameter.AbstractBeanParameter> parameters) {
      this.goal = goal;
      this.parameters = parameters;
    }
    @Override
    <R> R accept(ValidGoalCases<R> cases) {
      return cases.beanGoal(this);
    }
  }

  static final Function<ValidGoal, AbstractGoal> abstractGoal =
      asFunction(new ValidGoalCases<AbstractGoal>() {
        @Override
        public AbstractGoal regularGoal(ValidRegularGoal goal) {
          return goal.goal.goal;
        }
        @Override
        public AbstractGoal beanGoal(ValidBeanGoal goal) {
          return goal.goal.goal;
        }
      });

  static String goalName(ValidGoal goal) {
    return abstractGoal.apply(goal).name;
  }

  static TypeName goalType(ValidGoal goal) {
    return DtoGoal.goalType.apply(DtoValidGoal.abstractGoal.apply(goal));
  }

  private DtoValidGoal() {
    throw new UnsupportedOperationException("no instances");
  }
}
