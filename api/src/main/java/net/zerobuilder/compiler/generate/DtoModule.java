package net.zerobuilder.compiler.generate;

import net.zerobuilder.compiler.generate.DtoMethodGoal.SimpleStaticMethodGoalContext;
import net.zerobuilder.compiler.generate.DtoModuleOutput.AbstractModuleOutput;
import net.zerobuilder.compiler.generate.DtoSimpleGoal.SimpleGoal;

public final class DtoModule {

  public static abstract class Module {
    protected abstract AbstractModuleOutput process(SimpleGoal goal);
  }

  public static abstract class RegularContractModule {
    protected abstract AbstractModuleOutput process(SimpleStaticMethodGoalContext goal);
  }

  public static abstract class ProjectedModule {
    protected abstract AbstractModuleOutput process(DtoProjectedGoal.ProjectedGoal goal);
  }

  private DtoModule() {
    throw new UnsupportedOperationException("no instances");
  }
}
