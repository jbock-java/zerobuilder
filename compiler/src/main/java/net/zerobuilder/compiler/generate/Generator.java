package net.zerobuilder.compiler.generate;

import com.palantir.javapoet.ClassName;
import com.palantir.javapoet.MethodSpec;
import com.palantir.javapoet.TypeSpec;
import java.util.List;
import java.util.Set;
import net.zerobuilder.compiler.generate.DtoGeneratorInput.AbstractGoalInput;
import net.zerobuilder.compiler.generate.DtoGeneratorInput.BuilderGoalInput;
import net.zerobuilder.compiler.generate.DtoGeneratorInput.UpdaterGoalInput;
import net.zerobuilder.compiler.generate.DtoGeneratorOutput.GeneratorOutput;

import static java.util.stream.Collectors.toSet;

public final class Generator {

  /**
   * Entry point for code generation.
   *
   * @param goals inputs, may not be empty, must all have the same goal context
   * @return a GeneratorOutput
   * @throws IllegalArgumentException if input is invalid
   */
  public static GeneratorOutput generate(List<AbstractGoalInput> goals) {
    if (goals.isEmpty()) {
      throw new IllegalArgumentException("no input");
    }
    Set<ClassName> generatedType = goals.stream()
        .map(DtoGeneratorInput::getContext)
        .map(GoalContext::generatedType)
        .collect(toSet());
    if (generatedType.size() != 1) {
      throw new IllegalArgumentException("generated type is ambiguous");
    }
    List<ModuleOutput> tmpOutputs = goals.stream()
        .filter(Generator::hasParameters)
        .map(Generator::process)
        .toList();
    return new GeneratorOutput(
        methods(tmpOutputs),
        types(tmpOutputs),
        generatedType.iterator().next());
  }

  static boolean hasParameters(AbstractGoalInput goalInput) {
    return switch (goalInput) {
      case BuilderGoalInput builder -> !builder.description().parameters().isEmpty();
      case UpdaterGoalInput updater -> !updater.description().parameters().isEmpty();
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

  private static ModuleOutput process(AbstractGoalInput goalInput) {
    return switch (goalInput) {
      case UpdaterGoalInput projected -> projected.module().process(projected.description());
      case BuilderGoalInput regular -> regular.module().process(regular.description());
    };
  }

  private Generator() {
    throw new UnsupportedOperationException("no instances");
  }
}
