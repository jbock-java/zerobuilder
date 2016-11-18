package net.zerobuilder.compiler.generate;

import net.zerobuilder.compiler.generate.DtoMethodGoal.SimpleStaticMethodGoalContext;
import net.zerobuilder.compiler.generate.DtoModuleOutput.ModuleOutput;
import net.zerobuilder.compiler.generate.DtoProjectedGoal.ProjectedGoal;
import net.zerobuilder.compiler.generate.DtoRegularGoal.SimpleRegularGoalContext;
import net.zerobuilder.compiler.generate.DtoSimpleGoal.SimpleGoal;

public final class DtoModule {

  public static abstract class Module {
    protected abstract ModuleOutput process(SimpleGoal goal);
  }

  public static abstract class RegularContractModule {
    protected abstract ModuleOutput process(SimpleStaticMethodGoalContext goal);
  }

  public static abstract class ProjectedModule {
    protected abstract ModuleOutput process(ProjectedGoal goal);
  }

  public static abstract class RegularSimpleModule {
    protected abstract ModuleOutput process(SimpleRegularGoalContext goal);
  }

  private DtoModule() {
    throw new UnsupportedOperationException("no instances");
  }
}
