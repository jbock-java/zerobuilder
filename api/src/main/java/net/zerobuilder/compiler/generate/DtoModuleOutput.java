package net.zerobuilder.compiler.generate;

import com.squareup.javapoet.FieldSpec;
import com.squareup.javapoet.TypeSpec;
import net.zerobuilder.compiler.generate.DtoGeneratorOutput.BuilderMethod;

import java.util.List;

public final class DtoModuleOutput {

  public static abstract class AbstractModuleOutput {
    private final BuilderMethod method;
    private final List<TypeSpec> typeSpecs;
    private final List<FieldSpec> cacheFields;
    final BuilderMethod method() {
      return method;
    }
    final List<TypeSpec> typeSpecs() {
      return typeSpecs;
    }
    final List<FieldSpec> cacheFields() {
      return cacheFields;
    }
    protected AbstractModuleOutput(BuilderMethod method, List<TypeSpec> typeSpecs,
                                   List<FieldSpec> cacheFields) {
      this.method = method;
      this.typeSpecs = typeSpecs;
      this.cacheFields = cacheFields;
    }
  }

  public static final class ContractModuleOutput extends AbstractModuleOutput {

    public ContractModuleOutput(BuilderMethod method, List<TypeSpec> typeSpecs,
                                List<FieldSpec> cacheFields) {
      super(method, typeSpecs, cacheFields);
    }
  }

  private DtoModuleOutput() {
    throw new UnsupportedOperationException("no instances");
  }
}
