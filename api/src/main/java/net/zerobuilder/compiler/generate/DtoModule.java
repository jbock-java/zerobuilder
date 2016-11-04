package net.zerobuilder.compiler.generate;

import com.squareup.javapoet.ClassName;
import com.squareup.javapoet.FieldSpec;
import net.zerobuilder.compiler.generate.DtoMethodGoal.SimpleStaticMethodGoalContext;
import net.zerobuilder.compiler.generate.DtoModuleOutput.ContractModuleOutput;
import net.zerobuilder.compiler.generate.DtoModuleOutput.SimpleModuleOutput;
import net.zerobuilder.compiler.generate.DtoRegularGoal.SimpleRegularGoalContext;
import net.zerobuilder.compiler.generate.DtoSimpleGoal.SimpleGoal;
import net.zerobuilder.compiler.generate.DtoStep.AbstractStep;

import java.util.List;
import java.util.function.BiFunction;

import static javax.lang.model.element.Modifier.PRIVATE;
import static net.zerobuilder.compiler.generate.DtoSimpleGoal.abstractSteps;
import static net.zerobuilder.compiler.generate.DtoSimpleGoal.context;
import static net.zerobuilder.compiler.generate.DtoSimpleGoal.name;
import static net.zerobuilder.compiler.generate.ZeroUtil.downcase;
import static net.zerobuilder.compiler.generate.ZeroUtil.transform;
import static net.zerobuilder.compiler.generate.ZeroUtil.upcase;

public final class DtoModule {

  public static abstract class Module {
    protected abstract String name();
    abstract <R, P> R accept(ModuleCases<R, P> cases, P p);

    public final ClassName implType(SimpleGoal goal) {
      String implName = DtoModule.implName.apply(this, goal);
      return context.apply(goal)
          .generatedType.nestedClass(implName);
    }

    public final String methodName(SimpleGoal goal) {
      return name.apply(goal) + upcase(name());
    }

    // TODO this should not need goal argument
    public final FieldSpec cacheField(SimpleGoal goal) {
      ClassName type = implType(goal);
      return FieldSpec.builder(type, downcase(type.simpleName()), PRIVATE)
          .initializer("new $T()", type)
          .build();
    }

    protected final List<? extends AbstractStep> steps(SimpleGoal goal) {
      return abstractSteps.apply(goal);
    }
  }

  interface ModuleCases<R, P> {
    R simple(SimpleModule module, P p);
    R contract(ContractModule module, P p);
  }

  public static abstract class SimpleModule extends Module {

    protected abstract SimpleModuleOutput process(SimpleGoal goal);

    @Override
    public final <R, P> R accept(ModuleCases<R, P> cases, P p) {
      return cases.simple(this, p);
    }
  }

  public static abstract class ContractModule extends Module {
    protected abstract ContractModuleOutput process(SimpleGoal goal);

    @Override
    public final <R, P> R accept(ModuleCases<R, P> cases, P p) {
      return cases.contract(this, p);
    }
  }

  public static abstract class RegularContractModule {
    protected abstract ContractModuleOutput process(SimpleStaticMethodGoalContext goal);
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

  private static final BiFunction<Module, SimpleGoal, String> implName =
      moduleCases(
          (simple, goal) -> upcase(name.apply(goal)) + upcase(simple.name()),
          (contract, goal) -> upcase(name.apply(goal)) + upcase(contract.name()) + "Impl");

  private DtoModule() {
    throw new UnsupportedOperationException("no instances");
  }
}
