package net.zerobuilder.compiler.generate;

import com.squareup.javapoet.FieldSpec;
import com.squareup.javapoet.TypeSpec;
import net.zerobuilder.compiler.generate.DtoContext.BuildersContext;
import net.zerobuilder.compiler.generate.DtoDescriptionInput.DescriptionInput;
import net.zerobuilder.compiler.generate.DtoGeneratorInput.AbstractGoalInput;
import net.zerobuilder.compiler.generate.DtoGeneratorInput.GeneratorInput;
import net.zerobuilder.compiler.generate.DtoGeneratorOutput.BuilderMethod;
import net.zerobuilder.compiler.generate.DtoGeneratorOutput.GeneratorOutput;
import net.zerobuilder.compiler.generate.DtoInputOutput.AbstractInputOutput;
import net.zerobuilder.compiler.generate.DtoInputOutput.InputOutput;
import net.zerobuilder.compiler.generate.DtoInputOutput.ProjectedInputOutput;
import net.zerobuilder.compiler.generate.DtoInputOutput.SimpleRegularInputOutput;
import net.zerobuilder.compiler.generate.DtoModuleOutput.AbstractModuleOutput;

import java.util.List;
import java.util.function.Function;
import java.util.stream.Collector;

import static java.util.Collections.emptyList;
import static java.util.stream.Collectors.toList;
import static net.zerobuilder.compiler.generate.DtoContext.BuilderLifecycle.NEW_INSTANCE;
import static net.zerobuilder.compiler.generate.DtoGeneratorInput.goalInputCases;
import static net.zerobuilder.compiler.generate.GoalContextFactory.prepare;
import static net.zerobuilder.compiler.generate.ZeroUtil.concat;
import static net.zerobuilder.compiler.generate.ZeroUtil.flatList;
import static net.zerobuilder.compiler.generate.ZeroUtil.listCollector;

public final class Generator {

  /**
   * Entry point for code generation.
   *
   * @param generatorInput Goal descriptions
   * @return a GeneratorOutput
   */
  public static GeneratorOutput generate(GeneratorInput generatorInput) {
    List<DescriptionInput> goals = generatorInput.goals;
    BuildersContext context = generatorInput.context;
    return goals.stream()
        .map(prepare(context))
        .map(process)
        .collect(collectOutput(context));
  }

  private static Collector<AbstractInputOutput, List<AbstractInputOutput>, GeneratorOutput> collectOutput(BuildersContext context) {
    return listCollector(tmpOutputs ->
        GeneratorOutput.create(
            methods(tmpOutputs),
            types(tmpOutputs),
            fields(context, tmpOutputs),
            context));
  }

  private static List<BuilderMethod> methods(List<AbstractInputOutput> inputOutputs) {
    return inputOutputs.stream()
        .map(AbstractInputOutput::output)
        .map(AbstractModuleOutput::method)
        .collect(toList());
  }

  private static List<TypeSpec> types(List<AbstractInputOutput> inputOutputs) {
    return inputOutputs.stream()
        .map(AbstractInputOutput::output)
        .map(AbstractModuleOutput::typeSpecs)
        .collect(flatList());
  }

  private static List<FieldSpec> fields(BuildersContext context, List<AbstractInputOutput> inputOutputs) {
    if (context.lifecycle == NEW_INSTANCE) {
      return emptyList();
    }
    List<FieldSpec> fieldSpecs = inputOutputs.stream()
        .map(AbstractInputOutput::output)
        .map(AbstractModuleOutput::cacheFields)
        .collect(flatList());
    return concat(context.cache.get(), fieldSpecs);
  }

  private static final Function<AbstractGoalInput, AbstractInputOutput> process =
      goalInputCases(
          simple -> InputOutput.create(simple.module, simple.goal),
          projected -> ProjectedInputOutput.create(projected.module, projected.goal),
          regular -> SimpleRegularInputOutput.create(regular.module, regular.goal));

  private Generator() {
    throw new UnsupportedOperationException("no instances");
  }
}
