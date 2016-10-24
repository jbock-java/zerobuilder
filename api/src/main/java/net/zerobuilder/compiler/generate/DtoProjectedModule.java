package net.zerobuilder.compiler.generate;

import net.zerobuilder.compiler.generate.DtoGeneratorInput.ProjectedGoalInput;
import net.zerobuilder.compiler.generate.DtoGeneratorOutput.ProjectedSimpleModuleOutput;
import net.zerobuilder.compiler.generate.DtoGoalContext.AbstractGoalContext;
import net.zerobuilder.compiler.generate.DtoModuleOutput.ContractModuleOutput;
import net.zerobuilder.compiler.generate.DtoProjectedGoal.ProjectedGoal;

import java.util.function.BiFunction;
import java.util.function.Function;

import static net.zerobuilder.compiler.generate.Utilities.upcase;

public final class DtoProjectedModule {

  public static abstract class ProjectedModule {
    public abstract String name();
    public abstract <R, P> R accept(ProjectedModuleCases<R, P> cases, P p);
  }

  interface ProjectedModuleCases<R, P> {
    R simple(SimpleModule module, P p);
    R contract(ContractModule module, P p);
  }

  public static abstract class SimpleModule extends ProjectedModule {

    protected abstract ProjectedSimpleModuleOutput process(ProjectedGoal goal);

    @Override
    public final <R, P> R accept(ProjectedModuleCases<R, P> cases, P p) {
      return cases.simple(this, p);
    }
  }

  public static abstract class ContractModule extends ProjectedModule {
    protected abstract ContractModuleOutput process(AbstractGoalContext goal);

    @Override
    public final <R, P> R accept(ProjectedModuleCases<R, P> cases, P p) {
      return cases.contract(this, p);
    }
  }

  static <R, P> BiFunction<ProjectedModule, P, R> asFunction(ProjectedModuleCases<R, P> cases) {
    return (module, p) -> module.accept(cases, p);
  }

  static <R, P> BiFunction<ProjectedModule, P, R> moduleCases(
      BiFunction<SimpleModule, P, R> simple,
      BiFunction<ContractModule, P, R> contract) {
    return asFunction(new ProjectedModuleCases<R, P>() {
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

  static <R> Function<ProjectedGoalInput, R> goalInputCases(
      BiFunction<SimpleModule, ProjectedGoal, R> simple,
      BiFunction<ContractModule, ProjectedGoal, R> contract) {
    return input -> asFunction(new ProjectedModuleCases<R, ProjectedGoal>() {
      @Override
      public R simple(SimpleModule module, ProjectedGoal p) {
        return simple.apply(module, p);
      }
      @Override
      public R contract(ContractModule module, ProjectedGoal p) {
        return contract.apply(module, p);
      }
    }).apply(input.module, input.goal);
  }

  private static final BiFunction<ProjectedModule, AbstractGoalContext, String> implName =
      moduleCases(
          (simple, goal) -> upcase(goal.name()) + upcase(simple.name()),
          (contract, goal) -> upcase(goal.name()) + upcase(contract.name()) + "Impl");

  private static final BiFunction<ProjectedModule, AbstractGoalContext, String> contractName =
      moduleCases(
          (simple, goal) -> {
            throw new IllegalStateException("simple modules do not have a contract");
          },
          (contract, goal) -> upcase(goal.name()) + upcase(contract.name()));

  private DtoProjectedModule() {
    throw new UnsupportedOperationException("no instances");
  }
}
