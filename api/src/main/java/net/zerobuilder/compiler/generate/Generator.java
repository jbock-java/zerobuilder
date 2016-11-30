package net.zerobuilder.compiler.generate;

import com.squareup.javapoet.FieldSpec;
import com.squareup.javapoet.TypeSpec;
import net.zerobuilder.compiler.generate.DtoContext.GoalContext;
import net.zerobuilder.compiler.generate.DtoDescriptionInput.DescriptionInput;
import net.zerobuilder.compiler.generate.DtoGeneratorInput.AbstractGoalInput;
import net.zerobuilder.compiler.generate.DtoGeneratorInput.GeneratorInput;
import net.zerobuilder.compiler.generate.DtoGeneratorOutput.BuilderMethod;
import net.zerobuilder.compiler.generate.DtoGeneratorOutput.GeneratorOutput;
import net.zerobuilder.compiler.generate.DtoModuleOutput.ModuleOutput;

import java.util.List;
import java.util.function.Function;
import java.util.stream.Collector;

import static java.util.Collections.emptyList;
import static java.util.stream.Collectors.toList;
import static net.zerobuilder.compiler.generate.DtoContext.ContextLifecycle.NEW_INSTANCE;
import static net.zerobuilder.compiler.generate.DtoDescriptionInput.descriptionInputCases;
import static net.zerobuilder.compiler.generate.DtoGeneratorInput.goalInputCases;
import static net.zerobuilder.compiler.generate.GoalContextFactory.prepare;
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
    DtoContext.GoalContext context = generatorInput.context;
    return goals.stream()
        .filter(goal -> hasParameters.apply(goal))
        .map(prepare(context))
        .map(process)
        .collect(collectOutput(context));
  }

  private static final Function<DescriptionInput, Boolean> hasParameters =
      descriptionInputCases(
          (m, regular) -> !regular.parameters().isEmpty(),
          (m, projected) -> !projected.parameters().isEmpty(),
          (m, bean) -> !bean.parameters().isEmpty());

  private static Collector<ModuleOutput, List<ModuleOutput>, GeneratorOutput> collectOutput(GoalContext context) {
    return listCollector(tmpOutputs ->
        GeneratorOutput.create(
            methods(tmpOutputs),
            types(tmpOutputs),
            fields(context, tmpOutputs),
            context));
  }

  private static List<BuilderMethod> methods(List<ModuleOutput> inputOutputs) {
    return inputOutputs.stream()
        .map(ModuleOutput::method)
        .collect(toList());
  }

  private static List<TypeSpec> types(List<ModuleOutput> inputOutputs) {
    return inputOutputs.stream()
        .map(ModuleOutput::typeSpecs)
        .collect(flatList());
  }

  private static List<FieldSpec> fields(GoalContext context, List<ModuleOutput> inputOutputs) {
    if (context.lifecycle == NEW_INSTANCE) {
      return emptyList();
    }
    return inputOutputs.stream()
        .map(ModuleOutput::cacheFields)
        .collect(flatList());
  }

  private static final Function<AbstractGoalInput, ModuleOutput> process =
      goalInputCases(
          projected -> projected.module.process(projected.goal),
          regularSimple -> regularSimple.module.process(regularSimple.goal),
          bean -> bean.module.process(bean.goal));


  private Generator() {
    throw new UnsupportedOperationException("no instances");
  }
}
