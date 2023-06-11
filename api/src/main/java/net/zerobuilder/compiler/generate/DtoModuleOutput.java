package net.zerobuilder.compiler.generate;

import io.jbock.javapoet.FieldSpec;
import io.jbock.javapoet.TypeSpec;
import net.zerobuilder.compiler.generate.DtoGeneratorOutput.BuilderMethod;

import java.util.List;

public final class DtoModuleOutput {

  public static final class ModuleOutput {
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

    public ModuleOutput(BuilderMethod method,
                        List<TypeSpec> typeSpecs,
                        List<FieldSpec> cacheFields) {
      this.method = method;
      this.typeSpecs = typeSpecs;
      this.cacheFields = cacheFields;
    }
  }

  private DtoModuleOutput() {
    throw new UnsupportedOperationException("no instances");
  }
}
