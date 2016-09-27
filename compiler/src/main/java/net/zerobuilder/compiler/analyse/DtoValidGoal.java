package net.zerobuilder.compiler.analyse;

import com.google.common.collect.ImmutableList;
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

  private DtoValidGoal() {
    throw new UnsupportedOperationException("no instances");
  }
}
