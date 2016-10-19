package net.zerobuilder.compiler.generate;

import com.squareup.javapoet.TypeName;
import net.zerobuilder.NullPolicy;
import net.zerobuilder.compiler.generate.DtoParameter.AbstractParameter;
import net.zerobuilder.compiler.generate.DtoParameter.ParameterCases;
import net.zerobuilder.compiler.generate.DtoProjectionInfo.ProjectionInfo;

import java.util.Optional;
import java.util.function.Function;

public final class DtoRegularParameter {

  interface RegularParameterCases<R> {
    R simpleParameter(SimpleParameter parameter);
    R projectedParameter(ProjectedParameter parameter);
  }

  static <R> Function<AbstractRegularParameter, R> asFunction(RegularParameterCases<R> cases) {
    return parameter -> parameter.acceptRegularParameter(cases);
  }

  static <R> Function<AbstractRegularParameter, R> regularParameterCases(
      Function<SimpleParameter, R> simpleParameter,
      Function<ProjectedParameter, R> projectedParameter) {
    return asFunction(new RegularParameterCases<R>() {
      @Override
      public R simpleParameter(SimpleParameter parameter) {
        return simpleParameter.apply(parameter);
      }
      @Override
      public R projectedParameter(ProjectedParameter parameter) {
        return projectedParameter.apply(parameter);
      }
    });
  }

  private static final Function<AbstractRegularParameter, Optional<ProjectionInfo>> projectionInfo =
      regularParameterCases(
          simpleParameter -> Optional.empty(),
          projectedParameter -> Optional.of(projectedParameter.projectionInfo));

  /**
   * Represents one method (or constructor) parameter.
   */
  public static abstract class AbstractRegularParameter extends AbstractParameter {

    /**
     * original parameter name
     */
    final String name;

    private AbstractRegularParameter(String name, TypeName type, NullPolicy nullPolicy) {
      super(type, nullPolicy);
      this.name = name;
    }

    public final Optional<ProjectionInfo> projectionInfo() {
      return projectionInfo.apply(this);
    }

    @Override
    public final String name() {
      return name;
    }

    @Override
    public final <R> R acceptParameter(ParameterCases<R> cases) {
      return cases.regularParameter(this);
    }

    public abstract <R> R acceptRegularParameter(RegularParameterCases<R> cases);
  }

  public static final class ProjectedParameter extends AbstractRegularParameter {

    final ProjectionInfo projectionInfo;

    private ProjectedParameter(String name, TypeName type, NullPolicy nullPolicy, ProjectionInfo projectionInfo) {
      super(name, type, nullPolicy);
      this.projectionInfo = projectionInfo;
    }

    @Override
    public <R> R acceptRegularParameter(RegularParameterCases<R> cases) {
      return cases.projectedParameter(this);
    }
  }

  public static final class SimpleParameter extends AbstractRegularParameter {
    private SimpleParameter(String name, TypeName type, NullPolicy nullPolicy) {
      super(name, type, nullPolicy);
    }

    @Override
    public <R> R acceptRegularParameter(RegularParameterCases<R> cases) {
      return cases.simpleParameter(this);
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
