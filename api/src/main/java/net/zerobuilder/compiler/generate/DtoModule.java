package net.zerobuilder.compiler.generate;

import com.squareup.javapoet.ClassName;
import com.squareup.javapoet.FieldSpec;
import net.zerobuilder.compiler.generate.DtoGoalContext.AbstractGoalContext;
import net.zerobuilder.compiler.generate.DtoModuleOutput.ContractModuleOutput;
import net.zerobuilder.compiler.generate.DtoModuleOutput.SimpleModuleOutput;
import net.zerobuilder.compiler.generate.DtoProjectedGoal.ProjectedGoal;

import java.util.List;
import java.util.function.BiFunction;
import java.util.function.Function;

import static javax.lang.model.element.Modifier.FINAL;
import static javax.lang.model.element.Modifier.PRIVATE;
import static net.zerobuilder.compiler.generate.DtoGoalContext.abstractSteps;
import static net.zerobuilder.compiler.generate.DtoGoalContext.context;
import static net.zerobuilder.compiler.generate.Utilities.downcase;
import static net.zerobuilder.compiler.generate.Utilities.transform;
import static net.zerobuilder.compiler.generate.Utilities.upcase;

public final class DtoModule {

  public static abstract class Module {
    public abstract String name();
    public abstract <R, P> R accept(ModuleCases<R, P> cases, P p);

    final ClassName implType(AbstractGoalContext goal) {
      String implName = DtoModule.implName.apply(this, goal);
      return context.apply(goal)
          .generatedType.nestedClass(implName);
    }

    final String methodName(AbstractGoalContext goal) {
      return goal.name() + upcase(name());
    }

    final ClassName contractType(AbstractGoalContext goal) {
      String contractName = DtoModule.contractName.apply(this, goal);
      return context.apply(goal)
          .generatedType.nestedClass(contractName);
    }

    final FieldSpec cacheField(AbstractGoalContext goal) {
      ClassName type = implType(goal);
      return FieldSpec.builder(type, downcase(type.simpleName()), PRIVATE, FINAL)
          .initializer("new $T()", type)
          .build();
    }

    final List<ClassName> stepInterfaceTypes(AbstractGoalContext goal) {
      return transform(steps(goal), step -> contractType(goal).nestedClass(step.thisType));
    }

    final List<DtoStep.AbstractStep> steps(AbstractGoalContext goal) {
      return abstractSteps.apply(goal);
    }
  }

  interface ModuleCases<R, P> {
    R simple(SimpleModule module, P p);
    R contract(ContractModule module, P p);
  }

  public static abstract class SimpleModule extends Module {

    protected abstract SimpleModuleOutput process(AbstractGoalContext goal);

    @Override
    public final <R, P> R accept(ModuleCases<R, P> cases, P p) {
      return cases.simple(this, p);
    }
  }

  public static abstract class ProjectedSimpleModule extends Module {

    protected abstract DtoGeneratorOutput.ProjectedSimpleModuleOutput process(ProjectedGoal goal);
  }

  public static abstract class ContractModule extends Module {
    protected abstract ContractModuleOutput process(AbstractGoalContext goal);

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

  static <R> Function<GeneratorInput.AbstractGoalInput, R> goalInputCases(
      BiFunction<SimpleModule, DtoGoalContext.AbstractGoalContext, R> simple,
      BiFunction<ContractModule, DtoGoalContext.AbstractGoalContext, R> contract) {
    return input -> asFunction(new ModuleCases<R, DtoGoalContext.AbstractGoalContext>() {
      @Override
      public R simple(SimpleModule module, DtoGoalContext.AbstractGoalContext p) {
        return simple.apply(module, p);
      }
      @Override
      public R contract(ContractModule module, DtoGoalContext.AbstractGoalContext p) {
        return contract.apply(module, p);
      }
    }).apply(input.module, input.goal);
  }

  private static final BiFunction<Module, AbstractGoalContext, String> implName =
      moduleCases(
          (simple, goal) -> upcase(goal.name()) + upcase(simple.name()),
          (contract, goal) -> upcase(goal.name()) + upcase(contract.name()) + "Impl");

  private static final BiFunction<Module, AbstractGoalContext, String> contractName =
      moduleCases(
          (simple, goal) -> {
            throw new IllegalStateException("contractName");
          },
          (contract, goal) -> upcase(goal.name()) + upcase(contract.name()));

  private DtoModule() {
    throw new UnsupportedOperationException("no instances");
  }
}
