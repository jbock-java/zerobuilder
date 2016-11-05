package net.zerobuilder.compiler.generate;

import net.zerobuilder.compiler.generate.DtoGoalContext.AbstractGoalContext;
import net.zerobuilder.compiler.generate.DtoProjectedGoal.ProjectedGoal;

import java.util.List;

import static net.zerobuilder.compiler.generate.DtoGoalContext.abstractSteps;
import static net.zerobuilder.compiler.generate.ZeroUtil.upcase;

public final class DtoProjectedModule {

  public static abstract class ProjectedModule {
    protected abstract String name();

    protected final AbstractGoalContext goalContext(ProjectedGoal goal) {
      return DtoProjectedGoal.goalContext.apply(goal);
    }

    protected final String methodName(ProjectedGoal goal) {
      return legacyMethodName(goalContext(goal));
    }

    @Deprecated
    public final String legacyMethodName(AbstractGoalContext goal) {
      return goal.name() + upcase(name());
    }

    protected abstract DtoModuleOutput.SimpleModuleOutput process(ProjectedGoal goal);

    protected final List<? extends DtoStep.AbstractStep> steps(ProjectedGoal goal) {
      return abstractSteps.apply(goalContext(goal));
    }
  }

  private DtoProjectedModule() {
    throw new UnsupportedOperationException("no instances");
  }
}
