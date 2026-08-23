package net.zerobuilder.compiler.generate;

import com.palantir.javapoet.MethodSpec;
import com.palantir.javapoet.TypeSpec;
import java.util.List;

public record ModuleOutput(
    MethodSpec method,
    List<TypeSpec> typeSpecs) {
}
