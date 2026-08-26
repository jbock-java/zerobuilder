package net.zerobuilder.compiler.generate;

import com.palantir.javapoet.TypeName;
import net.zerobuilder.compiler.generate.DtoProjectionInfo.ProjectionInfo;

public record ProjectedParameter(
    String name,
    TypeName type,
    ProjectionInfo projectionInfo
) {
}
