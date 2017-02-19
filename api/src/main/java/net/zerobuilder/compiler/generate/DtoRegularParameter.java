package net.zerobuilder.compiler.generate;

import com.squareup.javapoet.TypeName;
import net.zerobuilder.compiler.generate.DtoProjectionInfo.ProjectionInfo;

public final class DtoRegularParameter {

  /**
   * Represents one method (or constructor) parameter.
   */
  static abstract class AbstractRegularParameter {

    /**
     * original parameter type
     */
    public final TypeName type;

    /**
     * original parameter name
     */
    public final String name;

    private AbstractRegularParameter(String name, TypeName type) {
      this.type = type;
      this.name = name;
    }

    public final String name() {
      return name;
    }
  }

  public static final class ProjectedParameter extends AbstractRegularParameter {

    public final ProjectionInfo projectionInfo;

    private ProjectedParameter(String name, TypeName type, ProjectionInfo projectionInfo) {
      super(name, type);
      this.projectionInfo = projectionInfo;
    }
  }

  public static final class SimpleParameter extends AbstractRegularParameter {
    private SimpleParameter(String name, TypeName type) {
      super(name, type);
    }
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
