package net.zerobuilder.compiler.generate;

import com.squareup.javapoet.TypeName;

import java.util.List;

public class DtoConstructorGoal {

  static final class ConstructorGoalContext
      extends DtoRegularGoal.AbstractRegularGoalContext {

    final DtoIConstructorGoal.ConstructorGoal goal;
    final DtoContext.BuildersContext context;

    ConstructorGoalContext(DtoIConstructorGoal.ConstructorGoal goal,
                           DtoContext.BuildersContext context) {
      this.goal = goal;
      this.context = context;
    }

    @Override
    public <R> R acceptRegular(DtoRegularGoal.RegularGoalContextCases<R> cases) {
      return cases.constructorGoal(this);
    }

    @Override
    public List<String> parameterNames() {
      return goal.details.parameterNames;
    }

    @Override
    public TypeName type() {
      return goal.details.goalType;
    }

    @Override
    public <R> R accept(DtoGoalContext.GoalCases<R> cases) {
      return cases.regularGoal(this);
    }
  }

  private DtoConstructorGoal() {
    throw new UnsupportedOperationException("no instances");
  }
}
