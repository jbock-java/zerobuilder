package net.zerobuilder.modules.updater;

import io.jbock.simple.Component;
import net.zerobuilder.compiler.generate.GoalDescription;
import net.zerobuilder.compiler.generate.ModuleOutput;

@Component
public interface UpdaterComponent {
  RegularUpdater createRegularUpdater();

  @Component.Factory
  interface Factory {
    UpdaterComponent create(GoalDescription description);
  }

  static ModuleOutput process(GoalDescription description) {
    return UpdaterComponent_Impl.factory()
        .create(description)
        .createRegularUpdater()
        .process();
  }
}
