package net.zerobuilder.compiler.analyse;

import com.google.common.collect.ImmutableList;
import net.zerobuilder.compiler.analyse.DtoPackage.GoalTypes.RegularGoalElement;
import net.zerobuilder.compiler.analyse.DtoShared.ValidRegularParameter;

final class DtoValidGoal {

  static abstract class ValidGoal {
    abstract <R> R accept(ValidationResultCases<R> cases);
  }

  interface ValidationResultCases<R> {
    R regularGoal(RegularGoalElement goal, ImmutableList<ValidRegularParameter> parameters);
    R beanGoal(DtoPackage.GoalTypes.BeanGoalElement beanGoal, ImmutableList<DtoShared.ValidBeanParameter> validBeanParameters);
  }

  static final class ValidRegularGoal extends ValidGoal {
    private final RegularGoalElement goal;
    private final ImmutableList<ValidRegularParameter> parameters;
    ValidRegularGoal(RegularGoalElement goal, ImmutableList<ValidRegularParameter> parameters) {
      this.goal = goal;
      this.parameters = parameters;
    }
    @Override
    <R> R accept(ValidationResultCases<R> cases) {
      return cases.regularGoal(goal, parameters);
    }
  }

  static final class ValidBeanGoal extends ValidGoal {
    private final DtoPackage.GoalTypes.BeanGoalElement goal;
    private final ImmutableList<DtoShared.ValidBeanParameter> validBeanParameters;
    ValidBeanGoal(DtoPackage.GoalTypes.BeanGoalElement goal, ImmutableList<DtoShared.ValidBeanParameter> validBeanParameters) {
      this.goal = goal;
      this.validBeanParameters = validBeanParameters;
    }
    @Override
    <R> R accept(ValidationResultCases<R> cases) {
      return cases.beanGoal(goal, validBeanParameters);
    }
  }

  private DtoValidGoal() {
    throw new UnsupportedOperationException("no instances");
  }
}
