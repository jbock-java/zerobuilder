package net.zerobuilder.compiler.analyse;

import com.google.common.collect.ImmutableList;
import net.zerobuilder.compiler.analyse.DtoGoalElement.BeanGoalElement;
import net.zerobuilder.compiler.analyse.DtoGoalElement.RegularGoalElement;
import net.zerobuilder.compiler.analyse.DtoValidParameter.ValidRegularParameter;

final class DtoValidGoal {

  static abstract class ValidGoal {
    abstract <R> R accept(ValidGoalCases<R> cases);
  }

  interface ValidGoalCases<R> {
    R regularGoal(ValidRegularGoal goal);
    R beanGoal(ValidBeanGoal goal);
  }

  static final class ValidRegularGoal extends ValidGoal {
    final RegularGoalElement goal;
    final ImmutableList<ValidRegularParameter> parameters;
    ValidRegularGoal(RegularGoalElement goal, ImmutableList<ValidRegularParameter> parameters) {
      this.goal = goal;
      this.parameters = parameters;
    }
    @Override
    <R> R accept(ValidGoalCases<R> cases) {
      return cases.regularGoal(this);
    }
  }

  static final class ValidBeanGoal extends ValidGoal {
    final BeanGoalElement goal;
    final ImmutableList<DtoBeanParameter.ValidBeanParameter> parameters;
    ValidBeanGoal(BeanGoalElement goal, ImmutableList<DtoBeanParameter.ValidBeanParameter> parameters) {
      this.goal = goal;
      this.parameters = parameters;
    }
    @Override
    <R> R accept(ValidGoalCases<R> cases) {
      return cases.beanGoal(this);
    }
  }

  private DtoValidGoal() {
    throw new UnsupportedOperationException("no instances");
  }
}
