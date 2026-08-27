package net.zerobuilder.compiler.generate;

import com.palantir.javapoet.ClassName;
import net.zerobuilder.compiler.generate.DtoGeneratorOutput.GeneratorOutput;
import net.zerobuilder.modules.builder.BuilderComponent;
import net.zerobuilder.modules.updater.UpdaterComponent;

import java.util.stream.Stream;

public final class Generator {

  /**
   * Entry point for code generation.
   *
   * @param goal inputs, may not be empty, must all have the same goal context
   * @return a GeneratorOutput
   * @throws IllegalArgumentException if input is invalid
   */
  public static GeneratorOutput generate(GoalDescription goal) {
    ClassName generatedType = goal.generatedType();
    ModuleOutput tmpOutput = Generator.process(goal);
    return new GeneratorOutput(goal.details(), tmpOutput.method(), tmpOutput.typeSpecs(), generatedType);
  }

  private static ModuleOutput process(GoalDescription description) {
    ModuleOutput builderOutput = BuilderComponent.process(description);
    ModuleOutput updaterOutput = UpdaterComponent.process(description);
    return new ModuleOutput(Stream.concat(builderOutput.method().stream(), updaterOutput.method().stream()).toList(), Stream.concat(builderOutput.typeSpecs().stream(), updaterOutput.typeSpecs().stream()).toList());
  }

  private Generator() {
  }
}
