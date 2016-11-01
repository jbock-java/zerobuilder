package net.zerobuilder.compiler.generate;

import com.squareup.javapoet.ClassName;
import com.squareup.javapoet.FieldSpec;
import net.zerobuilder.compiler.generate.DtoGeneratorInput.ProjectedGoalInput;
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
import static net.zerobuilder.compiler.generate.ZeroUtil.downcase;
import static net.zerobuilder.compiler.generate.ZeroUtil.transform;
import static net.zerobuilder.compiler.generate.ZeroUtil.upcase;

public final class DtoProjectedModule {

  interface ProjectedModuleCases<R, P> {
    R simple(ProjectedSimpleModule module, P p);
    R contract(ProjectedContractModule module, P p);
  }

  static abstract class ProjectedModule {
    protected abstract String name();

    protected final AbstractGoalContext goalContext(ProjectedGoal goal) {
      return DtoProjectedGoal.goalContext.apply(goal);
    }

    public final ClassName implType(ProjectedGoal goal) {
      return legacyImplType(goalContext(goal));
    }

    @Deprecated
    public final ClassName legacyImplType(AbstractGoalContext goal) {
      String implName = DtoProjectedModule.implName.apply(this, goal);
      return context.apply(goal)
          .generatedType.nestedClass(implName);
    }

    protected final String methodName(ProjectedGoal goal) {
      return legacyMethodName(goalContext(goal));
    }

    @Deprecated
    public final String legacyMethodName(AbstractGoalContext goal) {
      return goal.name() + upcase(name());
    }

    public final FieldSpec cacheField(ProjectedGoal goal) {
      return legacyCacheField(goalContext(goal));
    }

    @Deprecated
    public final FieldSpec legacyCacheField(AbstractGoalContext goal) {
      ClassName type = legacyImplType(goal);
      return FieldSpec.builder(type, downcase(type.simpleName()), PRIVATE)
          .initializer("new $T()", type)
          .build();
    }

    protected final List<? extends DtoStep.AbstractStep> steps(ProjectedGoal goal) {
      return abstractSteps.apply(goalContext(goal));
    }


    abstract <R, P> R accept(ProjectedModuleCases<R, P> cases, P p);
  }

  public static abstract class ProjectedSimpleModule extends ProjectedModule {

    protected abstract SimpleModuleOutput process(ProjectedGoal goal);

    @Override
    public final <R, P> R accept(ProjectedModuleCases<R, P> cases, P p) {
      return cases.simple(this, p);
    }
  }

  public static abstract class ProjectedContractModule extends ProjectedModule {

    protected abstract ContractModuleOutput process(ProjectedGoal goal);

    protected final ClassName contractType(ProjectedGoal goal) {
      String contractName = upcase(goalContext(goal).name()) + upcase(name());
      return context.apply(goalContext(goal))
          .generatedType.nestedClass(contractName);
    }

    protected final List<ClassName> stepInterfaceTypes(ProjectedGoal goal) {
      return transform(steps(goal), step -> contractType(goal).nestedClass(step.thisType));
    }

    @Override
    public final <R, P> R accept(ProjectedModuleCases<R, P> cases, P p) {
      return cases.contract(this, p);
    }
  }

  static <R, P> BiFunction<ProjectedModule, P, R> asFunction(ProjectedModuleCases<R, P> cases) {
    return (module, p) -> module.accept(cases, p);
  }

  static <R, P> BiFunction<ProjectedModule, P, R> moduleCases(
      BiFunction<ProjectedSimpleModule, P, R> simple,
      BiFunction<ProjectedContractModule, P, R> contract) {
    return asFunction(new ProjectedModuleCases<R, P>() {
      @Override
      public R simple(ProjectedSimpleModule module, P p) {
        return simple.apply(module, p);
      }
      @Override
      public R contract(ProjectedContractModule module, P p) {
        return contract.apply(module, p);
      }
    });
  }

  static <R> Function<ProjectedGoalInput, R> projectedGoalInputCases(
      BiFunction<ProjectedSimpleModule, ProjectedGoal, R> simple,
      BiFunction<ProjectedContractModule, ProjectedGoal, R> contract) {
    return input -> asFunction(new ProjectedModuleCases<R, ProjectedGoal>() {
      @Override
      public R simple(ProjectedSimpleModule module, ProjectedGoal p) {
        return simple.apply(module, p);
      }
      @Override
      public R contract(ProjectedContractModule module, ProjectedGoal p) {
        return contract.apply(module, p);
      }
    }).apply(input.module, input.goal);
  }

  private static final BiFunction<ProjectedModule, AbstractGoalContext, String> implName =
      moduleCases(
          (simple, goal) -> upcase(goal.name()) + upcase(simple.name()),
          (contract, goal) -> upcase(goal.name()) + upcase(contract.name()) + "Impl");

  private DtoProjectedModule() {
    throw new UnsupportedOperationException("no instances");
  }
}
