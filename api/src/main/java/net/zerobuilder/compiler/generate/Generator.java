package net.zerobuilder.compiler.generate;

import com.squareup.javapoet.FieldSpec;
import com.squareup.javapoet.TypeSpec;
import net.zerobuilder.compiler.generate.DtoContext.BuildersContext;
import net.zerobuilder.compiler.generate.DtoGeneratorOutput.BuilderMethod;
import net.zerobuilder.compiler.generate.DtoGeneratorOutput.GeneratorOutput;
import net.zerobuilder.compiler.generate.DtoGoalContext.AbstractGoalContext;
import net.zerobuilder.compiler.generate.DtoModule.Module;
import net.zerobuilder.compiler.generate.DtoModuleOutput.ModuleOutput;

import java.util.List;
import java.util.function.BiFunction;
import java.util.function.Function;

import static java.util.Arrays.asList;
import static java.util.Collections.emptyList;
import static java.util.Collections.singletonList;
import static java.util.stream.Collectors.toList;
import static net.zerobuilder.compiler.generate.DtoContext.BuilderLifecycle.NEW_INSTANCE;
import static net.zerobuilder.compiler.generate.DtoModule.moduleCases;
import static net.zerobuilder.compiler.generate.DtoModuleOutput.moduleOutputCases;
import static net.zerobuilder.compiler.generate.GoalContextFactory.prepare;
import static net.zerobuilder.compiler.generate.Utilities.concat;
import static net.zerobuilder.compiler.generate.Utilities.flatList;
import static net.zerobuilder.compiler.generate.Utilities.transform;
import static net.zerobuilder.compiler.generate.Utilities.upcase;

public final class Generator {

  /**
   * Entry point for code generation.
   *
   * @param generatorInput Goal descriptions
   * @return a GeneratorOutput
   */
  public static GeneratorOutput generate(GeneratorInput generatorInput) {
    return generate(generatorInput.context,
        transform(generatorInput.goals, prepare(generatorInput.context)));
  }

  private static GeneratorOutput generate(BuildersContext context, List<AbstractGoalContext> goals) {
    List<ModuleOutput> outputs = transform(goals, process);
    return new GeneratorOutput(
        methods(outputs),
        nestedTypes(outputs),
        fields(context, goals),
        context.generatedType,
        context.lifecycle);
  }

  private static final Function<AbstractGoalContext, ModuleOutput> process =
      goal -> Generator.biProcess.apply(goal.module(), goal);

  private static List<FieldSpec> fields(BuildersContext context, List<AbstractGoalContext> goals) {
    return context.lifecycle == NEW_INSTANCE ?
        emptyList() :
        concat(
            context.cache.get(),
            goals.stream()
                .map(AbstractGoalContext::cacheField)
                .collect(toList()));
  }

  private static final BiFunction<Module, AbstractGoalContext, ModuleOutput> biProcess =
      moduleCases(
          (simple, goal) -> simple.process(goal),
          (contract, goal) -> contract.process(goal));

  private static final Function<ModuleOutput, BuilderMethod> builderMethod =
      moduleOutputCases(
          simple -> simple.method,
          contract -> contract.method);

  private static final Function<ModuleOutput, List<TypeSpec>> nestedTypes =
      moduleOutputCases(
          simple -> singletonList(simple.impl),
          contract -> asList(contract.impl, contract.contract));

  private static List<BuilderMethod> methods(List<ModuleOutput> goals) {
    return transform(goals, builderMethod);
  }

  private static List<TypeSpec> nestedTypes(List<ModuleOutput> goals) {
    return goals.stream()
        .map(nestedTypes)
        .collect(flatList());
  }

  static final BiFunction<Module, AbstractGoalContext, String> implName =
      moduleCases(
          (simple, goal) -> upcase(goal.name()) + upcase(simple.name()),
          (contract, goal) -> upcase(goal.name()) + upcase(contract.name()) + "Impl");

  static final BiFunction<Module, AbstractGoalContext, String> contractName =
      moduleCases(
          (simple, goal) -> {
            throw new IllegalStateException("contractName");
          },
          (contract, goal) -> upcase(goal.name()) + upcase(contract.name()));

  private Generator() {
    throw new UnsupportedOperationException("no instances");
  }
}
