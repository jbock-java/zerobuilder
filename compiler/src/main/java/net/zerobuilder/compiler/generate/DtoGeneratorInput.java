package net.zerobuilder.compiler.generate;

import net.zerobuilder.compiler.generate.DtoModule.BuilderModule;
import net.zerobuilder.compiler.generate.DtoModule.UpdaterModule;
import net.zerobuilder.compiler.generate.DtoRegularGoalDescription.BuilderGoalDescription;
import net.zerobuilder.compiler.generate.DtoRegularGoalDescription.UpdaterGoalDescription;

public final class DtoGeneratorInput {

  public sealed interface AbstractGoalInput permits UpdaterGoalInput, BuilderGoalInput {
  }

  public record BuilderGoalInput(
      BuilderModule module,
      BuilderGoalDescription description
  ) implements AbstractGoalInput {
  }

  public record UpdaterGoalInput(
      UpdaterModule module,
      UpdaterGoalDescription description
  ) implements AbstractGoalInput {
  }

  static GoalContext getContext(AbstractGoalInput goalInput) {
    return switch (goalInput) {
      case UpdaterGoalInput updater -> updater.description.context();
      case BuilderGoalInput builder -> builder.description.context();
    };
  }

  private DtoGeneratorInput() {
    throw new UnsupportedOperationException("no instances");
  }
}
