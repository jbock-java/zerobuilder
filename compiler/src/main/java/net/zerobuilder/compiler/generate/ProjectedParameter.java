package net.zerobuilder.compiler.generate;

import com.palantir.javapoet.TypeName;

public record ProjectedParameter(
    String name,
    TypeName type,
    DtoProjectionInfo.ProjectionInfo projectionInfo
) {
}
