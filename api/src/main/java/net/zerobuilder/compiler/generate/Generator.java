package net.zerobuilder.compiler.generate;

import com.squareup.javapoet.FieldSpec;
import com.squareup.javapoet.TypeSpec;
import net.zerobuilder.compiler.generate.DtoContext.BuildersContext;
import net.zerobuilder.compiler.generate.DtoGeneratorOutput.BuilderMethod;
import net.zerobuilder.compiler.generate.DtoGeneratorOutput.GeneratorOutput;
import net.zerobuilder.compiler.generate.DtoGoalContext.AbstractGoalContext;

import java.util.Arrays;
import java.util.List;
import java.util.function.BiFunction;

import static java.util.Collections.emptyList;
import static java.util.Collections.singletonList;
import static java.util.stream.Collectors.toList;
import static net.zerobuilder.compiler.generate.DtoContext.BuilderLifecycle.NEW_INSTANCE;
import static net.zerobuilder.compiler.generate.GoalContextFactory.prepare;
import static net.zerobuilder.compiler.generate.Utilities.concat;
import static net.zerobuilder.compiler.generate.Utilities.flatList;
import static net.zerobuilder.compiler.generate.Utilities.transform;
import static net.zerobuilder.compiler.generate.Utilities.upcase;

public final class Generator {

  public interface ModuleOutput {
    <R> R accept(ModuleOutputCases<R> cases);
  }

  public interface ModuleOutputCases<R> {
    R simple(SimpleModuleOutput simple);
    R contract(ContractModuleOutput contract);
  }

  public static abstract class AbstractModuleOutput {
    final BuilderMethod method;
    final TypeSpec impl;
    protected AbstractModuleOutput(BuilderMethod method, TypeSpec impl) {
      this.method = method;
      this.impl = impl;
    }
  }

  public static final class SimpleModuleOutput extends AbstractModuleOutput {

    protected SimpleModuleOutput(BuilderMethod method, TypeSpec impl) {
      super(method, impl);
    }
  }

  public static final class ContractModuleOutput extends AbstractModuleOutput {
    final TypeSpec contract;

    protected ContractModuleOutput(BuilderMethod method, TypeSpec impl, TypeSpec contract) {
      super(method, impl);
      this.contract = contract;
    }
  }

  public interface Module {
    BuilderMethod method(AbstractGoalContext goal);
    TypeSpec impl(AbstractGoalContext goal);
    String name();
    <R, P> R accept(ModuleCases<R, P> cases, P p);
  }

  interface ModuleCases<R, P> {
    R simple(SimpleModule module, P p);
    R contract(ContractModule module, P p);
  }

  public static abstract class SimpleModule implements Module {

    @Override
    public final <R, P> R accept(ModuleCases<R, P> cases, P p) {
      return cases.simple(this, p);
    }
  }

  public static abstract class ContractModule implements Module {
    public abstract TypeSpec contract(AbstractGoalContext goal);

    @Override
    public final <R, P> R accept(ModuleCases<R, P> cases, P p) {
      return cases.contract(this, p);
    }
  }

  static <R, P> BiFunction<Module, P, R> asFunction(ModuleCases<R, P> cases) {
    return (module, p) -> module.accept(cases, p);
  }

  static <R, P> BiFunction<Module, P, R> moduleCases(
      BiFunction<SimpleModule, P, R> simple,
      BiFunction<ContractModule, P, R> contract) {
    return asFunction(new ModuleCases<R, P>() {
      @Override
      public R simple(SimpleModule module, P p) {
        return simple.apply(module, p);
      }
      @Override
      public R contract(ContractModule module, P p) {
        return contract.apply(module, p);
      }
    });
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
                .map(AbstractGoalContext::cacheField)
                .collect(toList()));
  }

  private static List<BuilderMethod> methods(List<AbstractGoalContext> goals) {
    return goals.stream()
        .map(goal -> goal.goalOption().module.method(goal))
        .collect(toList());
  }

  private static List<TypeSpec> nestedTypes(List<AbstractGoalContext> goals) {
    return goals.stream()
        .map(goal -> nestedTypes.apply(goal.goalOption().module, goal))
        .collect(flatList());
  }

  private static final BiFunction<Module, AbstractGoalContext, List<TypeSpec>> nestedTypes =
      moduleCases(
          (simple, goal) -> singletonList(simple.impl(goal)),
          (contract, goal) -> Arrays.asList(contract.impl(goal), contract.contract(goal)));

  static final BiFunction<Module, AbstractGoalContext, String> implName =
      moduleCases(
          (simple, goal) -> upcase(goal.name()) + upcase(simple.name()),
          (contract, goal) -> upcase(goal.name()) + upcase(contract.name()) + "Impl");

}
