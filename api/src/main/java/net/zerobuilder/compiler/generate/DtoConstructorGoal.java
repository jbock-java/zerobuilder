package net.zerobuilder.compiler.generate;

import com.squareup.javapoet.TypeName;

import java.util.List;

public class DtoConstructorGoal {

  static final class ConstructorGoalContext
      extends DtoRegularGoal.AbstractRegularGoalContext {

    final DtoContext.BuildersContext context;
    final DtoGoal.ConstructorGoalDetails details;

    final List<DtoRegularStep.AbstractRegularStep> steps;
    final List<TypeName> thrownTypes;

    ConstructorGoalContext(DtoContext.BuildersContext context,
                           DtoGoal.ConstructorGoalDetails details,
                           List<DtoRegularStep.AbstractRegularStep> steps,
                           List<TypeName> thrownTypes) {
      this.context = context;
      this.details = details;
      this.steps = steps;
      this.thrownTypes = thrownTypes;
    }

    @Override
    public <R> R acceptRegular(DtoRegularGoal.RegularGoalContextCases<R> cases) {
      return cases.constructorGoal(this);
    }

    @Override
    public List<String> parameterNames() {
      return details.parameterNames;
    }

    @Override
    public TypeName type() {
      return details.goalType;
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
