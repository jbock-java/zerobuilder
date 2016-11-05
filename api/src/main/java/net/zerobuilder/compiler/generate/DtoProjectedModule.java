package net.zerobuilder.compiler.generate;

import net.zerobuilder.compiler.generate.DtoModuleOutput.SimpleModuleOutput;
import net.zerobuilder.compiler.generate.DtoProjectedGoal.ProjectedGoal;

public final class DtoProjectedModule {

  public static abstract class ProjectedModule {
    protected abstract SimpleModuleOutput process(ProjectedGoal goal);
  }

  private DtoProjectedModule() {
    throw new UnsupportedOperationException("no instances");
  }
}
