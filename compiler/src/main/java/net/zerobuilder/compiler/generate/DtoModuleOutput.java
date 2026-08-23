package net.zerobuilder.compiler.generate;

import com.palantir.javapoet.FieldSpec;
import com.palantir.javapoet.TypeSpec;
import net.zerobuilder.compiler.generate.DtoGeneratorOutput.BuilderMethod;

import java.util.List;

public final class DtoModuleOutput {

  public record ModuleOutput(BuilderMethod method, List<TypeSpec> typeSpecs, List<FieldSpec> cacheFields) {
  }

  private DtoModuleOutput() {
    throw new UnsupportedOperationException("no instances");
  }
}
