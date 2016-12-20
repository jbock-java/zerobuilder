package net.zerobuilder.compiler.generate;

import com.squareup.javapoet.ClassName;
import com.squareup.javapoet.FieldSpec;
import com.squareup.javapoet.TypeSpec;
import net.zerobuilder.compiler.generate.DtoGeneratorInput.AbstractGoalInput;
import net.zerobuilder.compiler.generate.DtoGeneratorOutput.BuilderMethod;
import net.zerobuilder.compiler.generate.DtoGeneratorOutput.GeneratorOutput;
import net.zerobuilder.compiler.generate.DtoModuleOutput.ModuleOutput;

import java.util.List;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collector;

import static java.util.stream.Collectors.toList;
import static java.util.stream.Collectors.toSet;
import static net.zerobuilder.compiler.generate.DtoGeneratorInput.goalInputCases;
import static net.zerobuilder.compiler.generate.ZeroUtil.flatList;
import static net.zerobuilder.compiler.generate.ZeroUtil.listCollector;

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
        .map(DtoGeneratorInput.getContext)
        .map(context -> context.generatedType)
        .collect(toSet());
    if (generatedType.size() != 1) {
      throw new IllegalArgumentException("generated type is ambiguous");
    }
    return goals.stream()
        .filter(hasParameters::apply)
        .map(process)
        .collect(collectOutput(generatedType.iterator().next()));
  }

  private static final Function<AbstractGoalInput, Boolean> hasParameters =
      goalInputCases(
          projected -> !projected.description.parameters.isEmpty(),
          regular -> !regular.description.parameters.isEmpty(),
          bean -> !bean.description.parameters.isEmpty());

  private static Collector<ModuleOutput, List<ModuleOutput>, GeneratorOutput> collectOutput(ClassName generatedType) {
    return listCollector(tmpOutputs ->
        GeneratorOutput.create(
            methods(tmpOutputs),
            types(tmpOutputs),
            fields(tmpOutputs),
            generatedType));
  }

  private static List<BuilderMethod> methods(List<ModuleOutput> outputs) {
    return outputs.stream()
        .map(ModuleOutput::method)
        .collect(toList());
  }

  private static List<TypeSpec> types(List<ModuleOutput> outputs) {
    return outputs.stream()
        .map(ModuleOutput::typeSpecs)
        .collect(flatList());
  }

  private static List<FieldSpec> fields(List<ModuleOutput> outputs) {
    return outputs.stream()
        .map(ModuleOutput::cacheFields)
        .collect(flatList());
  }

  private static final Function<AbstractGoalInput, ModuleOutput> process =
      goalInputCases(
          projected -> projected.module.process(projected.description),
          regularSimple -> regularSimple.module.process(regularSimple.description),
          bean -> bean.module.process(bean.description));


  private Generator() {
    throw new UnsupportedOperationException("no instances");
  }
}
