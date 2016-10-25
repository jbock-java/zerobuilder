package net.zerobuilder.compiler.generate;

import com.squareup.javapoet.ClassName;
import com.squareup.javapoet.FieldSpec;
import net.zerobuilder.compiler.generate.DtoGeneratorInput.AbstractGoalInput;
import net.zerobuilder.compiler.generate.DtoGoalContext.AbstractGoalContext;
import net.zerobuilder.compiler.generate.DtoModuleOutput.ContractModuleOutput;
import net.zerobuilder.compiler.generate.DtoModuleOutput.SimpleModuleOutput;

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
    protected abstract String name();
    abstract <R, P> R accept(ModuleCases<R, P> cases, P p);

    protected final ClassName implType(AbstractGoalContext goal) {
      String implName = DtoModule.implName.apply(this, goal);
      return context.apply(goal)
          .generatedType.nestedClass(implName);
    }

    protected final String methodName(AbstractGoalContext goal) {
      return goal.name() + upcase(name());
    }

    protected final FieldSpec cacheField(AbstractGoalContext goal) {
      ClassName type = implType(goal);
      return FieldSpec.builder(type, downcase(type.simpleName()), PRIVATE, FINAL)
          .initializer("new $T()", type)
          .build();
    }

    protected final List<DtoStep.AbstractStep> steps(AbstractGoalContext goal) {
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

  public static abstract class ContractModule extends Module {
    protected abstract ContractModuleOutput process(AbstractGoalContext goal);

    protected final List<ClassName> stepInterfaceTypes(AbstractGoalContext goal) {
      return transform(steps(goal), step -> contractType(goal).nestedClass(step.thisType));
    }

    protected final ClassName contractType(AbstractGoalContext goal) {
      String contractName = upcase(goal.name()) + upcase(name());
      return context.apply(goal)
          .generatedType.nestedClass(contractName);
    }

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

  private static final BiFunction<Module, AbstractGoalContext, String> implName =
      moduleCases(
          (simple, goal) -> upcase(goal.name()) + upcase(simple.name()),
          (contract, goal) -> upcase(goal.name()) + upcase(contract.name()) + "Impl");

  private DtoModule() {
    throw new UnsupportedOperationException("no instances");
  }
}
