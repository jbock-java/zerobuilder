package net.zerobuilder.compiler.generate;

import net.zerobuilder.compiler.generate.DtoMethodGoal.SimpleStaticMethodGoalContext;
import net.zerobuilder.compiler.generate.DtoModuleOutput.ContractModuleOutput;
import net.zerobuilder.compiler.generate.DtoSimpleGoal.SimpleGoal;

public final class DtoModule {

  public static abstract class Module {
    protected abstract ContractModuleOutput process(SimpleGoal goal);
  }

  public static abstract class RegularContractModule {
    protected abstract ContractModuleOutput process(SimpleStaticMethodGoalContext goal);
  }

  public static abstract class ProjectedModule {
    protected abstract ContractModuleOutput process(DtoProjectedGoal.ProjectedGoal goal);
  }

  private DtoModule() {
    throw new UnsupportedOperationException("no instances");
  }
}
