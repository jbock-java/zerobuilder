package net.zerobuilder.compiler.generate;

import com.palantir.javapoet.ClassName;
import com.palantir.javapoet.MethodSpec;
import com.palantir.javapoet.TypeSpec;
import net.zerobuilder.compiler.generate.DtoGeneratorOutput.GeneratorOutput;
import net.zerobuilder.compiler.generate.DtoRegularGoalDescription.AbstractGoalDescription;
import net.zerobuilder.compiler.generate.DtoRegularGoalDescription.BuilderGoalDescription;
import net.zerobuilder.compiler.generate.DtoRegularGoalDescription.UpdaterGoalDescription;
import net.zerobuilder.modules.builder.RegularBuilder;
import net.zerobuilder.modules.updater.RegularUpdater;

import java.util.List;

public final class Generator {

  private static final RegularBuilder BUILDER = new RegularBuilder();
  private static final RegularUpdater UPDATER = new RegularUpdater();

  /**
   * Entry point for code generation.
   *
   * @param goals inputs, may not be empty, must all have the same goal context
   * @return a GeneratorOutput
   * @throws IllegalArgumentException if input is invalid
   */
  public static GeneratorOutput generate(AbstractGoalDescription goals) {
    GoalDetails details = DtoRegularGoalDescription.getDetails(goals);
    ClassName generatedType = DtoRegularGoalDescription.getContext(goals).generatedType();
    ModuleOutput tmpOutput = Generator.process(goals);
    return new GeneratorOutput(
        details,
        List.of(tmpOutput.method()),
        tmpOutput.typeSpecs(),
        generatedType);
  }

  static boolean hasParameters(AbstractGoalDescription description) {
    return switch (description) {
      case BuilderGoalDescription builder -> !builder.parameters().isEmpty();
      case UpdaterGoalDescription updater -> !updater.parameters().isEmpty();
    };
  }

  private static List<MethodSpec> methods(List<ModuleOutput> outputs) {
    return outputs.stream()
        .map(ModuleOutput::method)
        .toList();
  }

  private static List<TypeSpec> types(List<ModuleOutput> outputs) {
    return outputs.stream()
        .map(ModuleOutput::typeSpecs)
        .flatMap(List::stream)
        .toList();
  }

  private static ModuleOutput process(AbstractGoalDescription description) {
    return switch (description) {
      case BuilderGoalDescription builder -> BUILDER.process(builder);
      case UpdaterGoalDescription updater -> UPDATER.process(updater);
    };
  }

  private Generator() {
    throw new UnsupportedOperationException("no instances");
  }
}
