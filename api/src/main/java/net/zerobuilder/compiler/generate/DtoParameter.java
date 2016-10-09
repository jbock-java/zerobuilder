package net.zerobuilder.compiler.generate;

import com.squareup.javapoet.TypeName;
import net.zerobuilder.NullPolicy;
import net.zerobuilder.compiler.generate.DtoBeanParameter.AbstractBeanParameter;
import net.zerobuilder.compiler.generate.DtoProjectionInfo.ProjectionInfo;

import java.util.function.Function;

import static net.zerobuilder.compiler.generate.DtoBeanParameter.beanParameterName;

public final class DtoParameter {

  interface ParameterCases<R> {
    R regularParameter(RegularParameter parameter);
    R beanParameter(AbstractBeanParameter parameter);
  }

  public static <R> Function<AbstractParameter, R> asFunction(final ParameterCases<R> cases) {
    return parameter -> parameter.acceptParameter(cases);
  }

  abstract static class AbstractParameter {

    /**
     * <p>for beans, this is the type that's returned by the getter,
     * or equivalently the type of the setter parameter</p>
     * <p>for regular goals, it is the original parameter type</p>
     */
    final TypeName type;

    /**
     * true if null checks should be added
     */
    final NullPolicy nullPolicy;

    AbstractParameter(TypeName type, NullPolicy nullPolicy) {
      this.type = type;
      this.nullPolicy = nullPolicy;
    }

    public abstract <R> R acceptParameter(ParameterCases<R> cases);
  }

  /**
   * Represents one of the parameters of a method or constructor.
   */
  public static final class RegularParameter extends AbstractParameter {

    /**
     * original parameter name
     */
    final String name;

    /**
     * projection info
     */
    final ProjectionInfo projectionInfo;

    private RegularParameter(String name, TypeName type, ProjectionInfo projectionInfo, NullPolicy nullPolicy) {
      super(type, nullPolicy);
      this.projectionInfo = projectionInfo;
      this.name = name;
    }

    /**
     * Creates a parameter without projection info.
     *
     * @param name       parameter name
     * @param type       parameter type
     * @param nullPolicy null policy
     * @return a parameter
     */
    public static RegularParameter create(String name, TypeName type, NullPolicy nullPolicy) {
      return new RegularParameter(name, type, DtoProjectionInfo.none(), nullPolicy);
    }

    /**
     * Creates a parameter.
     *
     * @param name           parameter name
     * @param type           parameter type
     * @param nullPolicy     null policy
     * @param projectionInfo projection info
     * @return a parameter
     */
    public static RegularParameter create(String name, TypeName type, NullPolicy nullPolicy, ProjectionInfo projectionInfo) {
      return new RegularParameter(name, type, projectionInfo, nullPolicy);
    }

    @Override
    public <R> R acceptParameter(ParameterCases<R> cases) {
      return cases.regularParameter(this);
    }
  }

  static final Function<AbstractParameter, String> parameterName
      = asFunction(new ParameterCases<String>() {
    @Override
    public String regularParameter(RegularParameter parameter) {
      return parameter.name;
    }
    @Override
    public String beanParameter(AbstractBeanParameter parameter) {
      return parameter.accept(beanParameterName);
    }
  });

  private DtoParameter() {
    throw new UnsupportedOperationException("no instances");
  }
}
