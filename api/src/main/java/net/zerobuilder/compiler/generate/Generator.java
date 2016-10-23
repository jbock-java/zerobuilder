package net.zerobuilder.compiler.generate;

import com.squareup.javapoet.FieldSpec;
import com.squareup.javapoet.TypeSpec;
import net.zerobuilder.compiler.generate.DtoContext.BuildersContext;
import net.zerobuilder.compiler.generate.DtoGeneratorInput.AbstractGoalInput;
import net.zerobuilder.compiler.generate.DtoGeneratorInput.DescriptionInput;
import net.zerobuilder.compiler.generate.DtoGeneratorInput.GeneratorInput;
import net.zerobuilder.compiler.generate.DtoGeneratorOutput.BuilderMethod;
import net.zerobuilder.compiler.generate.DtoGeneratorOutput.GeneratorOutput;
import net.zerobuilder.compiler.generate.DtoGoalContext.AbstractGoalContext;
import net.zerobuilder.compiler.generate.DtoModule.Module;
import net.zerobuilder.compiler.generate.DtoModuleOutput.AbstractModuleOutput;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Function;
import java.util.stream.Collector;

import static java.util.Arrays.asList;
import static java.util.Collections.emptyList;
import static java.util.Collections.singletonList;
import static net.zerobuilder.compiler.generate.DtoContext.BuilderLifecycle.NEW_INSTANCE;
import static net.zerobuilder.compiler.generate.DtoModule.goalInputCases;
import static net.zerobuilder.compiler.generate.DtoModuleOutput.moduleOutputCases;
import static net.zerobuilder.compiler.generate.GoalContextFactory.prepare;
import static net.zerobuilder.compiler.generate.Utilities.flatList;
import static net.zerobuilder.compiler.generate.Utilities.listCollector;
import static net.zerobuilder.compiler.generate.Utilities.transform;

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

  private static Collector<InputOutput, List<InputOutput>, GeneratorOutput> collectOutput(BuildersContext context) {
    return listCollector(tmpOutputs ->
        GeneratorOutput.create(
            methods(tmpOutputs),
            types(tmpOutputs),
            fields(context, tmpOutputs),
            context));
  }

  private static List<BuilderMethod> methods(List<InputOutput> inputOutputs) {
    return transform(inputOutputs, tmp -> tmp.output.method);
  }

  private static List<TypeSpec> types(List<InputOutput> inputOutputs) {
    return inputOutputs.stream()
        .map(tmp -> nestedTypes.apply(tmp.output))
        .collect(flatList());
  }

  private static List<FieldSpec> fields(BuildersContext context, List<InputOutput> inputOutputs) {
    if (context.lifecycle == NEW_INSTANCE) {
      return emptyList();
    }
    List<FieldSpec> fields = new ArrayList<>(inputOutputs.size() + 1);
    fields.add(context.cache.get());
    inputOutputs.forEach(tmp ->
        fields.add(tmp.module.cacheField(tmp.goal)));
    return fields;
  }

  private static final Function<AbstractGoalInput, InputOutput> process =
      goalInputCases(
          (simple, goal) -> new InputOutput(simple, goal, simple.process(goal)),
          (contract, goal) -> new InputOutput(contract, goal, contract.process(goal)));

  private static final Function<AbstractModuleOutput, List<TypeSpec>> nestedTypes =
      moduleOutputCases(
          simple -> singletonList(simple.impl),
          contract -> asList(contract.impl, contract.contract));

  private static final class InputOutput {
    private final Module module;
    private final AbstractGoalContext goal;
    private final AbstractModuleOutput output;

    private InputOutput(Module module, AbstractGoalContext goal, AbstractModuleOutput output) {
      this.module = module;
      this.goal = goal;
      this.output = output;
    }
  }

  private Generator() {
    throw new UnsupportedOperationException("no instances");
  }
}
