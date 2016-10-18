package net.zerobuilder.compiler.generate;

import com.squareup.javapoet.TypeSpec;
import net.zerobuilder.compiler.generate.DtoGoalContext.AbstractGoalContext;
import net.zerobuilder.compiler.generate.DtoModuleOutput.ContractModuleOutput;
import net.zerobuilder.compiler.generate.DtoModuleOutput.SimpleModuleOutput;

import java.util.function.BiFunction;

public final class DtoModule {

  public interface Module {
    String name();
    <R, P> R accept(ModuleCases<R, P> cases, P p);
  }

  interface ModuleCases<R, P> {
    R simple(SimpleModule module, P p);
    R contract(ContractModule module, P p);
  }

  public static abstract class SimpleModule implements Module {

    protected abstract SimpleModuleOutput process(AbstractGoalContext goal);

    @Override
    public final <R, P> R accept(ModuleCases<R, P> cases, P p) {
      return cases.simple(this, p);
    }
  }

  public static abstract class ContractModule implements Module {
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

  private DtoModule() {
    throw new UnsupportedOperationException("no instances");
  }
}
