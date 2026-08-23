package net.zerobuilder.compiler.generate;

import net.zerobuilder.compiler.generate.DtoRegularGoalDescription.BuilderGoalDescription;
import net.zerobuilder.compiler.generate.DtoRegularGoalDescription.UpdaterGoalDescription;

public final class DtoModule {

  public interface UpdaterModule {
    ModuleOutput process(UpdaterGoalDescription description);
  }

  public interface BuilderModule {
    ModuleOutput process(BuilderGoalDescription description);
  }

  private DtoModule() {
    throw new UnsupportedOperationException("no instances");
  }
}
