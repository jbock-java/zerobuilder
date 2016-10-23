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
import java.util.Set;
import java.util.function.BiConsumer;
import java.util.function.BinaryOperator;
import java.util.function.Function;
import java.util.function.Supplier;
import java.util.stream.Collector;

import static java.util.Arrays.asList;
import static java.util.Collections.emptySet;
import static java.util.Collections.singletonList;
import static net.zerobuilder.compiler.generate.DtoContext.BuilderLifecycle.REUSE_INSTANCES;
import static net.zerobuilder.compiler.generate.DtoModule.goalInputCases;
import static net.zerobuilder.compiler.generate.DtoModuleOutput.moduleOutputCases;
import static net.zerobuilder.compiler.generate.GoalContextFactory.prepare;
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
    Function<DescriptionInput, AbstractGoalInput> prepare = prepare(generatorInput.context);
    List<AbstractGoalInput> transform = transform(goals, prepare);
    return generate(generatorInput.context, transform);
  }

  private static final class Output {
    private final Module module;
    private final AbstractGoalContext goal;
    private final AbstractModuleOutput output;
    private Output(Module module, AbstractGoalContext goal, AbstractModuleOutput output) {
      this.module = module;
      this.goal = goal;
      this.output = output;
    }
  }

  private static GeneratorOutput generate(BuildersContext context, List<AbstractGoalInput> goals) {
    return goals.stream()
        .map(process)
        .collect(collectOutput(context));
  }

  static Collector<Output, List<Output>, GeneratorOutput> collectOutput(
      DtoContext.BuildersContext context) {
    return new Collector<Output, List<Output>, GeneratorOutput>() {

      @Override
      public Supplier<List<Output>> supplier() {
        return ArrayList::new;
      }

      @Override
      public BiConsumer<List<Output>, Output> accumulator() {
        return (left, right) -> left.add(right);
      }

      @Override
      public BinaryOperator<List<Output>> combiner() {
        return (left, right) -> {
          left.addAll(right);
          return left;
        };
      }

      @Override
      public Function<List<Output>, GeneratorOutput> finisher() {
        return outputs -> {
          List<BuilderMethod> methods = new ArrayList<>(outputs.size());
          List<TypeSpec> types = new ArrayList<>();
          List<FieldSpec> fields = new ArrayList<>();
          if (context.lifecycle == REUSE_INSTANCES) {
            fields.add(context.cache.get());
          }
          for (Output output : outputs) {
            methods.add(output.output.method);
            if (output.goal.context().lifecycle == REUSE_INSTANCES) {
              fields.add(output.module.cacheField(output.goal));
            }
            types.addAll(nestedTypes.apply(output.output));
          }
          return new GeneratorOutput(methods, types, fields, context.generatedType, context.lifecycle);
        };
      }

      @Override
      public Set<Characteristics> characteristics() {
        return emptySet();
      }
    };
  }

  private static final Function<AbstractGoalInput, Output> process =
      goalInputCases(
          (simple, goal) -> new Output(simple, goal, simple.process(goal)),
          (contract, goal) -> new Output(contract, goal, contract.process(goal)));

  private static final Function<AbstractModuleOutput, List<TypeSpec>> nestedTypes =
      moduleOutputCases(
          simple -> singletonList(simple.impl),
          contract -> asList(contract.impl, contract.contract));

  private Generator() {
    throw new UnsupportedOperationException("no instances");
  }
}
