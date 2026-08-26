package net.zerobuilder.compiler.generate;

import com.palantir.javapoet.ClassName;
import java.util.stream.Stream;
import net.zerobuilder.compiler.generate.DtoGeneratorOutput.GeneratorOutput;
import net.zerobuilder.modules.builder.RegularBuilder;
import net.zerobuilder.modules.updater.RegularUpdater;

public final class Generator {

  private static final RegularBuilder BUILDER = new RegularBuilder();
  private static final RegularUpdater UPDATER = new RegularUpdater();

  /**
   * Entry point for code generation.
   *
   * @param goal inputs, may not be empty, must all have the same goal context
   * @return a GeneratorOutput
   * @throws IllegalArgumentException if input is invalid
   */
  public static GeneratorOutput generate(GoalDescription goal) {
    ClassName generatedType = goal.context().generatedType();
    ModuleOutput tmpOutput = Generator.process(goal);
    return new GeneratorOutput(goal.details(), tmpOutput.method(), tmpOutput.typeSpecs(), generatedType);
  }

  private static ModuleOutput process(GoalDescription description) {
    ModuleOutput builderOutput = BUILDER.process(description);
    ModuleOutput updaterOutput = UPDATER.process(description);
    return new ModuleOutput(Stream.concat(builderOutput.method().stream(), updaterOutput.method().stream()).toList(), Stream.concat(builderOutput.typeSpecs().stream(), updaterOutput.typeSpecs().stream()).toList());
  }

  private Generator() {
    throw new UnsupportedOperationException("no instances");
  }
}
