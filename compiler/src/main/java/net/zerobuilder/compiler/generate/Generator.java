package net.zerobuilder.compiler.generate;

import java.util.stream.Stream;
import net.zerobuilder.modules.builder.BuilderComponent;
import net.zerobuilder.modules.updater.UpdaterComponent;

public final class Generator {

  /**
   * Entry point for code generation.
   *
   * @param goal inputs, may not be empty, must all have the same goal context
   * @return a GeneratorOutput
   * @throws IllegalArgumentException if input is invalid
   */
  public static GeneratorOutput generate(GoalDescription goal) {
    ModuleOutput moduleOutput = Generator.process(goal);
    return new GeneratorOutput(goal, moduleOutput);
  }

  private static ModuleOutput process(GoalDescription description) {
    ModuleOutput builderOutput = BuilderComponent.process(description);
    ModuleOutput updaterOutput = UpdaterComponent.process(description);
    return new ModuleOutput(
        Stream.concat(builderOutput.method().stream(), updaterOutput.method().stream())
            .toList(),
        Stream.concat(builderOutput.typeSpecs().stream(), updaterOutput.typeSpecs().stream())
            .toList());
  }

  private Generator() {
  }
}
