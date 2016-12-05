package net.zerobuilder.compiler.generate;

import net.zerobuilder.compiler.generate.DtoBeanGoalDescription.BeanGoalDescription;
import net.zerobuilder.compiler.generate.DtoModuleOutput.ModuleOutput;
import net.zerobuilder.compiler.generate.DtoRegularGoalDescription.ProjectedRegularGoalDescription;
import net.zerobuilder.compiler.generate.DtoRegularGoalDescription.SimpleRegularGoalDescription;

public final class DtoModule {

  public interface ProjectedModule {
    ModuleOutput process(ProjectedRegularGoalDescription description);
  }

  public interface RegularSimpleModule {
    ModuleOutput process(SimpleRegularGoalDescription description);
  }

  public interface BeanModule {
    ModuleOutput process(BeanGoalDescription description);
  }

  private DtoModule() {
    throw new UnsupportedOperationException("no instances");
  }
}
