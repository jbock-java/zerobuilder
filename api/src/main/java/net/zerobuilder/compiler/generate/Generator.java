package net.zerobuilder.compiler.generate;

import com.squareup.javapoet.FieldSpec;
import com.squareup.javapoet.TypeSpec;
import net.zerobuilder.compiler.generate.DtoContext.BuildersContext;
import net.zerobuilder.compiler.generate.DtoGeneratorOutput.BuilderMethod;
import net.zerobuilder.compiler.generate.DtoGeneratorOutput.GeneratorOutput;
import net.zerobuilder.compiler.generate.DtoGoalContext.AbstractGoalContext;
import net.zerobuilder.compiler.generate.DtoModule.Module;
import net.zerobuilder.compiler.generate.DtoModuleOutput.AbstractModuleOutput;
import net.zerobuilder.compiler.generate.GeneratorInput.AbstractGoalInput;
import net.zerobuilder.compiler.generate.GeneratorInput.DescriptionInput;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Function;
import java.util.stream.Collector;

import static java.util.Arrays.asList;
import static java.util.Collections.singletonList;
import static net.zerobuilder.compiler.generate.DtoContext.BuilderLifecycle.REUSE_INSTANCES;
import static net.zerobuilder.compiler.generate.DtoModule.goalInputCases;
import static net.zerobuilder.compiler.generate.DtoModuleOutput.moduleOutputCases;
import static net.zerobuilder.compiler.generate.GoalContextFactory.prepare;
import static net.zerobuilder.compiler.generate.Utilities.listCollector;

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

  private static Collector<TmpOutput, List<TmpOutput>, GeneratorOutput> collectOutput(BuildersContext context) {
    return listCollector(tmpOutputs -> {
      List<BuilderMethod> methods = new ArrayList<>(tmpOutputs.size());
      List<TypeSpec> types = new ArrayList<>();
      List<FieldSpec> fields = new ArrayList<>();
      if (context.lifecycle == REUSE_INSTANCES) {
        fields.add(context.cache.get());
      }
      for (TmpOutput tmp : tmpOutputs) {
        methods.add(tmp.output.method);
        if (tmp.goal.context().lifecycle == REUSE_INSTANCES) {
          fields.add(tmp.module.cacheField(tmp.goal));
        }
        types.addAll(nestedTypes.apply(tmp.output));
      }
      return new GeneratorOutput(methods, types, fields,
          context.generatedType, context.lifecycle);
    });
  }

  private static final Function<AbstractGoalInput, TmpOutput> process =
      goalInputCases(
          (simple, goal) -> new TmpOutput(simple, goal, simple.process(goal)),
          (contract, goal) -> new TmpOutput(contract, goal, contract.process(goal)));

  private static final Function<AbstractModuleOutput, List<TypeSpec>> nestedTypes =
      moduleOutputCases(
          simple -> singletonList(simple.impl),
          contract -> asList(contract.impl, contract.contract));

  private static final class TmpOutput {
    private final Module module;
    private final AbstractGoalContext goal;
    private final AbstractModuleOutput output;
    private TmpOutput(Module module, AbstractGoalContext goal, AbstractModuleOutput output) {
      this.module = module;
      this.goal = goal;
      this.output = output;
    }
  }

  private Generator() {
    throw new UnsupportedOperationException("no instances");
  }
}
