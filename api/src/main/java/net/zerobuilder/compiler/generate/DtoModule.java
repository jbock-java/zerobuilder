package net.zerobuilder.compiler.generate;

import net.zerobuilder.compiler.generate.DtoBeanGoal.BeanGoalContext;
import net.zerobuilder.compiler.generate.DtoModuleOutput.ModuleOutput;
import net.zerobuilder.compiler.generate.DtoProjectedRegularGoalContext.ProjectedRegularGoalContext;
import net.zerobuilder.compiler.generate.DtoRegularGoalDescription.SimpleRegularGoalDescription;

public final class DtoModule {

  public interface ProjectedModule {
    ModuleOutput process(ProjectedRegularGoalContext goal);
  }

  public interface RegularSimpleModule {
    ModuleOutput process(SimpleRegularGoalDescription description);
  }

  public interface BeanModule {
    ModuleOutput process(BeanGoalContext goal);
  }

  private DtoModule() {
    throw new UnsupportedOperationException("no instances");
  }
}
