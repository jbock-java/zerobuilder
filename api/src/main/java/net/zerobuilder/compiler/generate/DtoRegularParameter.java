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
     * true if null checks should be added
     */
    public final NullPolicy nullPolicy;

    /**
     * original parameter name
     */
    public final String name;

    private AbstractRegularParameter(String name, TypeName type, NullPolicy nullPolicy) {
      this.type = type;
      this.nullPolicy = nullPolicy;
      this.name = name;
    }

    public final String name() {
      return name;
    }
  }

  public static final class ProjectedParameter extends AbstractRegularParameter {

    public final ProjectionInfo projectionInfo;

    private ProjectedParameter(String name, TypeName type, NullPolicy nullPolicy, ProjectionInfo projectionInfo) {
      super(name, type, nullPolicy);
      this.projectionInfo = projectionInfo;
    }
  }

  public static final class SimpleParameter extends AbstractRegularParameter {
    private SimpleParameter(String name, TypeName type, NullPolicy nullPolicy) {
      super(name, type, nullPolicy);
    }
  }

  /**
   * Creates a parameter without projection info.
   *
   * @param name       parameter name
   * @param type       parameter type
   * @param nullPolicy null policy
   * @return a parameter
   */
  public static SimpleParameter create(String name, TypeName type, NullPolicy nullPolicy) {
    return new SimpleParameter(name, type, nullPolicy);
  }

  /**
   * Creates a parameter with projection info.
   *
   * @param name           parameter name
   * @param type           parameter type
   * @param nullPolicy     null policy
   * @param projectionInfo projection info
   * @return a parameter
   */
  public static ProjectedParameter create(String name, TypeName type, NullPolicy nullPolicy, ProjectionInfo projectionInfo) {
    return new ProjectedParameter(name, type, nullPolicy, projectionInfo);
  }

  private DtoRegularParameter() {
    throw new UnsupportedOperationException("no instances");
  }
}
