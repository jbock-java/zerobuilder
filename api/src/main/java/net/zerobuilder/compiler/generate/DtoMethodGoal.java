package net.zerobuilder.compiler.generate;

import net.zerobuilder.compiler.generate.DtoGoalDetails.InstanceMethodGoalDetails;
import net.zerobuilder.compiler.generate.DtoGoalDetails.StaticMethodGoalDetails;
import net.zerobuilder.compiler.generate.DtoRegularGoal.SimpleRegularGoalContext;
import net.zerobuilder.compiler.generate.DtoRegularGoalDescription.SimpleRegularGoalDescription;

public final class DtoMethodGoal {

  public static final class InstanceMethodGoalContext extends SimpleRegularGoalContext {

    public InstanceMethodGoalContext(
        InstanceMethodGoalDetails details,
        SimpleRegularGoalDescription description) {
      super(description);
      this.details = details;
    }

    public final InstanceMethodGoalDetails details;

    @Override
    public final <R> R acceptRegular(DtoRegularGoal.RegularGoalContextCases<R> cases) {
      return cases.instanceMethod(this);
    }
  }

  public static final class SimpleStaticMethodGoalContext extends SimpleRegularGoalContext {

    public SimpleStaticMethodGoalContext(
        StaticMethodGoalDetails details,
        SimpleRegularGoalDescription description) {
      super(description);
      this.details = details;
    }

    public final StaticMethodGoalDetails details;

    @Override
    public final <R> R acceptRegular(DtoRegularGoal.RegularGoalContextCases<R> cases) {
      return cases.staticMethod(this);
    }
  }

  private DtoMethodGoal() {
    throw new UnsupportedOperationException("no instances");
  }
}
