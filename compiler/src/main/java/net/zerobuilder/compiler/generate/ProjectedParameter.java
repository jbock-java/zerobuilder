package net.zerobuilder.compiler.generate;

import com.palantir.javapoet.TypeName;
import javax.lang.model.element.VariableElement;
import net.zerobuilder.StepName;
import net.zerobuilder.compiler.generate.DtoProjectionInfo.ProjectionInfo;

public record ProjectedParameter(
    String name,
    VariableElement parameter,
    ProjectionInfo projectionInfo
) {

  public TypeName type() {
    return TypeName.get(parameter.asType());
  }

  public static ProjectedParameter create(
      VariableElement parameter,
      ProjectionInfo projectionInfo) {
    StepName stepNameAnnotation = parameter.getAnnotation(StepName.class);
    String name = stepNameAnnotation == null ? parameter.getSimpleName().toString() : stepNameAnnotation.value();
    return new ProjectedParameter(name, parameter, projectionInfo);
  }
}
