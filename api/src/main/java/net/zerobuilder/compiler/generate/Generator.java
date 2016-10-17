package net.zerobuilder.compiler.generate;

import com.squareup.javapoet.FieldSpec;
import com.squareup.javapoet.TypeSpec;
import net.zerobuilder.compiler.generate.DtoContext.BuildersContext;
import net.zerobuilder.compiler.generate.DtoGeneratorOutput.BuilderMethod;
import net.zerobuilder.compiler.generate.DtoGeneratorOutput.GeneratorOutput;
import net.zerobuilder.compiler.generate.DtoGoalContext.AbstractGoalContext;

import java.util.List;

import static java.util.Collections.emptyList;
import static java.util.stream.Collectors.toList;
import static net.zerobuilder.compiler.generate.DtoContext.BuilderLifecycle.NEW_INSTANCE;
import static net.zerobuilder.compiler.generate.GoalContextFactory.prepare;
import static net.zerobuilder.compiler.generate.Utilities.concat;
import static net.zerobuilder.compiler.generate.Utilities.flatList;
import static net.zerobuilder.compiler.generate.Utilities.transform;

public final class Generator {

  public interface Module {
    BuilderMethod method(AbstractGoalContext goal);
    List<TypeSpec> nestedTypes(AbstractGoalContext goal);
    FieldSpec field(AbstractGoalContext goal);
    String name();
  }

  /**
   * Entry point for code generation.
   *
   * @param goals Goal descriptions
   * @return a GeneratorOutput
   */
  public static GeneratorOutput generate(GeneratorInput goals) {
    return generate(goals.context,
        transform(goals.goals, prepare(goals)));
  }

  private static GeneratorOutput generate(BuildersContext context, List<AbstractGoalContext> goals) {
    return new GeneratorOutput(
        methods(goals),
        nestedTypes(goals),
        fields(context, goals),
        context.generatedType,
        context.lifecycle);
  }

  private static List<FieldSpec> fields(BuildersContext context, List<AbstractGoalContext> goals) {
    return context.lifecycle == NEW_INSTANCE ?
        emptyList() :
        concat(
            context.cache.get(),
            goals.stream()
                .map(goal -> goal.module().module.field(goal))
                .collect(toList()));
  }

  private static List<BuilderMethod> methods(List<AbstractGoalContext> goals) {
    return goals.stream()
        .map(goal -> goal.module().module.method(goal))
        .collect(toList());
  }

  private static List<TypeSpec> nestedTypes(List<AbstractGoalContext> goals) {
    return goals.stream()
        .map(goal -> goal.module().module.nestedTypes(goal))
        .collect(flatList());
  }
}
