package net.zerobuilder.compiler.generate;

import com.squareup.javapoet.FieldSpec;
import com.squareup.javapoet.TypeSpec;
import net.zerobuilder.compiler.generate.DtoContext.BuildersContext;
import net.zerobuilder.compiler.generate.DtoGeneratorOutput.BuilderMethod;
import net.zerobuilder.compiler.generate.DtoGeneratorOutput.GeneratorOutput;
import net.zerobuilder.compiler.generate.DtoGeneratorOutput.SingleModuleOutput;
import net.zerobuilder.compiler.generate.DtoGeneratorOutput.SingleModuleOutputWithField;
import net.zerobuilder.compiler.generate.DtoGoalContext.AbstractGoalContext;
import net.zerobuilder.compiler.generate.DtoModule.Module;
import net.zerobuilder.compiler.generate.DtoModule.ProjectedModule;
import net.zerobuilder.compiler.generate.DtoModuleOutput.ModuleOutput;
import net.zerobuilder.compiler.generate.DtoProjectedGoal.ProjectedGoal;
import net.zerobuilder.compiler.generate.GeneratorInput.AbstractGoalInput;
import net.zerobuilder.compiler.generate.GeneratorInput.DescriptionInput;

import java.util.List;
import java.util.Optional;
import java.util.function.BiFunction;
import java.util.function.Function;

import static java.util.Arrays.asList;
import static java.util.Collections.emptyList;
import static java.util.Collections.singletonList;
import static java.util.Optional.empty;
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
    List<DescriptionInput> goals = generatorInput.goals;
    Function<DescriptionInput, AbstractGoalInput> prepare = prepare(generatorInput.context);
    List<AbstractGoalInput> transform = transform(goals, prepare);
    return generate(generatorInput.context, transform);
  }

  static SingleModuleOutputWithField invoke(ProjectedModule module,
                                            ProjectedGoal goal) {
    SingleModuleOutput output = module.process(goal);
    BuildersContext context = DtoProjectedGoal.context.apply(goal);
    Optional<FieldSpec> field = context.lifecycle == NEW_INSTANCE ?
        empty() :
        Optional.of(DtoProjectedGoal.cacheField.apply(goal));
    return new SingleModuleOutputWithField(output, field);
  }

/*
  static SingleModuleOutputWithField invoke(AbstractGoalContext goal) {
    SingleModuleOutput output = processSingle.apply(goal);
    BuildersContext context = goal.context();
    Optional<FieldSpec> field = context.lifecycle == NEW_INSTANCE ?
        empty() :
        Optional.of(goal.cacheField());
    return new SingleModuleOutputWithField(output, field);
  }
*/

  private static GeneratorOutput generate(BuildersContext context, List<AbstractGoalInput> goals) {
    List<ModuleOutput> outputs = transform(goals, process);
    return new GeneratorOutput(
        methods(outputs),
        nestedTypes(outputs),
        fields(context, goals),
        context.generatedType,
        context.lifecycle);
  }

  private static Function<AbstractGoalInput, ModuleOutput> process =
      input -> Generator.biProcess.apply(input.module, input.goal);


  private static Function<AbstractGoalContext, SingleModuleOutput> processSingle(Module module) {
    return goal -> Generator.singleBiProcess.apply(module, goal);
  }

  private static List<FieldSpec> fields(BuildersContext context, List<AbstractGoalInput> goals) {
    return context.lifecycle == NEW_INSTANCE ?
        emptyList() :
        concat(
            context.cache.get(),
            goals.stream()
                .map(input -> input.goal)
                .map(AbstractGoalContext::cacheField)
                .collect(toList()));
  }

  private static final BiFunction<Module, AbstractGoalContext, ModuleOutput> biProcess =
      moduleCases(
          (simple, goal) -> simple.process(goal),
          (contract, goal) -> contract.process(goal));

  private static final BiFunction<Module, AbstractGoalContext, SingleModuleOutput> singleBiProcess =
      moduleCases(
          (simple, goal) -> simple.processSingle(goal),
          (contract, goal) -> contract.processSingle(goal));

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
