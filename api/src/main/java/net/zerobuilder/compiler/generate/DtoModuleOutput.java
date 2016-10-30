package net.zerobuilder.compiler.generate;

import com.squareup.javapoet.TypeSpec;
import net.zerobuilder.compiler.generate.DtoGeneratorOutput.BuilderMethod;

import java.util.function.Function;

public final class DtoModuleOutput {

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
    public abstract <R> R accept(ModuleOutputCases<R> cases);
  }

  public static final class SimpleModuleOutput extends AbstractModuleOutput {

    public SimpleModuleOutput(BuilderMethod method, TypeSpec impl) {
      super(method, impl);
    }
    @Override
    public <R> R accept(ModuleOutputCases<R> cases) {
      return cases.simple(this);
    }
  }

  public static final class ContractModuleOutput extends AbstractModuleOutput {
    final TypeSpec contract;

    protected ContractModuleOutput(BuilderMethod method, TypeSpec impl, TypeSpec contract) {
      super(method, impl);
      this.contract = contract;
    }
    @Override
    public <R> R accept(ModuleOutputCases<R> cases) {
      return cases.contract(this);
    }
  }

  static <R> Function<AbstractModuleOutput, R> asFunction(ModuleOutputCases<R> cases) {
    return moduleOutput -> moduleOutput.accept(cases);
  }

  static <R> Function<AbstractModuleOutput, R> moduleOutputCases(
      Function<SimpleModuleOutput, R> simple,
      Function<ContractModuleOutput, R> contract) {
    return asFunction(new ModuleOutputCases<R>() {
      @Override
      public R simple(SimpleModuleOutput simpleOutput) {
        return simple.apply(simpleOutput);
      }
      @Override
      public R contract(ContractModuleOutput contractOutput) {
        return contract.apply(contractOutput);
      }
    });
  }


  private DtoModuleOutput() {
    throw new UnsupportedOperationException("no instances");
  }
}
