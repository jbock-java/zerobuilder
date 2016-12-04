package net.zerobuilder.compiler.generate;

import net.zerobuilder.compiler.generate.DtoContext.GoalContext;
import net.zerobuilder.compiler.generate.DtoGoalDetails.ConstructorGoalDetails;
import net.zerobuilder.compiler.generate.DtoRegularGoal.RegularGoalContextCases;
import net.zerobuilder.compiler.generate.DtoRegularGoalDescription.SimpleRegularGoalDescription;

public final class DtoConstructorGoal {

  public static final class SimpleConstructorGoalContext
      extends DtoRegularGoal.SimpleRegularGoalContext {

    public final GoalContext context;
    public final ConstructorGoalDetails details;

    SimpleConstructorGoalContext(DtoContext.GoalContext context,
                                 ConstructorGoalDetails details,
                                 SimpleRegularGoalDescription description) {
      super(description);
      this.context = context;
      this.details = details;
    }

    @Override
    public final <R> R acceptRegular(RegularGoalContextCases<R> cases) {
      return cases.constructor(this);
    }
  }

  private DtoConstructorGoal() {
    throw new UnsupportedOperationException("no instances");
  }
}
