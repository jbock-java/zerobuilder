package net.zerobuilder.compiler.generate;

import com.squareup.javapoet.FieldSpec;
import com.squareup.javapoet.TypeSpec;
import net.zerobuilder.compiler.generate.DtoContext.BuildersContext;
import net.zerobuilder.compiler.generate.DtoGeneratorOutput.BuilderMethod;
import net.zerobuilder.compiler.generate.DtoGeneratorOutput.GeneratorOutput;
import net.zerobuilder.compiler.generate.DtoGoalContext.AbstractGoalContext;

import java.util.List;
import java.util.function.Function;

import static java.util.Collections.emptyList;
import static java.util.Collections.unmodifiableList;
import static java.util.stream.Collectors.toList;
import static net.zerobuilder.compiler.generate.DtoContext.BuilderLifecycle.NEW_INSTANCE;
import static net.zerobuilder.compiler.generate.GoalContextFactory.prepare;
import static net.zerobuilder.compiler.generate.Utilities.concat;
import static net.zerobuilder.compiler.generate.Utilities.flatList;
import static net.zerobuilder.compiler.generate.Utilities.transform;

public final class Generator {

  private final Function<AbstractGoalContext, List<FieldSpec>> fields;
  private final Function<AbstractGoalContext, List<TypeSpec>> nestedTypes;
  private final Function<AbstractGoalContext, List<BuilderMethod>> methods;

  private Generator(
      Function<AbstractGoalContext, List<FieldSpec>> fields,
      Function<AbstractGoalContext, List<TypeSpec>> nestedTypes,
      Function<AbstractGoalContext, List<BuilderMethod>> methods) {
    this.fields = fields;
    this.nestedTypes = nestedTypes;
    this.methods = methods;
  }

  public static Generator create(List<? extends Module> modules) {
    List<Module> m = unmodifiableList(modules);
    return new Generator(
        fieldsFunction(m),
        nestedTypesFunction(m),
        methodsFunction(m));
  }

  public interface Module {
    BuilderMethod method(AbstractGoalContext goal);
    List<TypeSpec> nestedTypes(AbstractGoalContext goal);
    FieldSpec field(AbstractGoalContext goal);
    boolean handles(AbstractGoalContext goal);
  }

  /**
   * Entry point for code generation.
   *
   * @param goals Goal descriptions
   * @return a GeneratorOutput
   */
  public GeneratorOutput generate(GeneratorInput goals) {
    return generate(goals.buildersContext,
        transform(goals.goals, prepare(goals.buildersContext, goals)));
  }

  private GeneratorOutput generate(BuildersContext context, List<AbstractGoalContext> goals) {
    return new GeneratorOutput(
        methods(goals),
        nestedTypes(goals),
        fields(context, goals),
        context.generatedType,
        context.lifecycle);
  }


  private static Function<AbstractGoalContext, List<BuilderMethod>> methodsFunction(List<Module> modules) {
    return goal -> modules.stream()
        .filter(module -> module.handles(goal))
        .map(module -> module.method(goal))
        .collect(toList());
  }

  private static Function<AbstractGoalContext, List<TypeSpec>> nestedTypesFunction(List<Module> modules) {
    return goal -> modules.stream()
        .filter(module -> module.handles(goal))
        .map(module -> module.nestedTypes(goal))
        .collect(flatList());
  }

  private static Function<AbstractGoalContext, List<FieldSpec>> fieldsFunction(List<Module> modules) {
    return goal -> modules.stream()
        .filter(module -> module.handles(goal))
        .map(module -> module.field(goal))
        .collect(toList());
  }

  private List<FieldSpec> fields(BuildersContext context, List<AbstractGoalContext> goals) {
    return context.lifecycle == NEW_INSTANCE ?
        emptyList() :
        concat(
            context.cache.get(),
            goals.stream()
                .map(fields)
                .collect(flatList()));
  }

  private List<BuilderMethod> methods(List<AbstractGoalContext> goals) {
    return goals.stream()
        .map(methods)
        .collect(flatList());
  }

  private List<TypeSpec> nestedTypes(List<AbstractGoalContext> goals) {
    return goals.stream()
        .map(nestedTypes)
        .collect(flatList());
  }
}
