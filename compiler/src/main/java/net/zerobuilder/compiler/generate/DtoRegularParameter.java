package net.zerobuilder.compiler.generate;

import com.palantir.javapoet.TypeName;
import net.zerobuilder.compiler.generate.DtoProjectionInfo.ProjectionInfo;

public final class DtoRegularParameter {

  /**
   * Represents one method (or constructor) parameter.
   */
  public sealed interface AbstractParameter permits SimpleParameter, ProjectedParameter {
    String name();

    TypeName type();
  }

  public record ProjectedParameter(
      String name,
      TypeName type,
      ProjectionInfo projectionInfo
  ) implements AbstractParameter {
  }

  /**
   * @param name original parameter name
   * @param type original parameter type
   */
  public record SimpleParameter(
      String name,
      TypeName type
  ) implements AbstractParameter {
  }

  /**
   * Creates a parameter without projection info.
   *
   * @param name parameter name
   * @param type parameter type
   * @return a parameter
   */
  public static SimpleParameter create(String name, TypeName type) {
    return new SimpleParameter(name, type);
  }

  /**
   * Creates a parameter with projection info.
   *
   * @param name           parameter name
   * @param type           parameter type
   * @param projectionInfo projection info
   * @return a parameter
   */
  public static ProjectedParameter create(String name, TypeName type, ProjectionInfo projectionInfo) {
    return new ProjectedParameter(name, type, projectionInfo);
  }

  private DtoRegularParameter() {
    throw new UnsupportedOperationException("no instances");
  }
}
