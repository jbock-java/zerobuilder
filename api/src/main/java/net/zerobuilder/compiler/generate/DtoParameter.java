package net.zerobuilder.compiler.generate;

import com.squareup.javapoet.TypeName;
import net.zerobuilder.NullPolicy;
import net.zerobuilder.compiler.generate.DtoBeanParameter.AbstractBeanParameter;
import net.zerobuilder.compiler.generate.DtoRegularParameter.AbstractRegularParameter;

import java.util.function.Function;

public final class DtoParameter {

  interface ParameterCases<R> {
    R regularParameter(AbstractRegularParameter parameter);
    R beanParameter(AbstractBeanParameter parameter);
  }

  static <R> Function<AbstractParameter, R> asFunction(ParameterCases<R> cases) {
    return parameter -> parameter.acceptParameter(cases);
  }

  static <R> Function<AbstractParameter, R> parameterCases(
      Function<AbstractRegularParameter, R> regularParameter,
      Function<AbstractBeanParameter, R> beanParameter) {
    return asFunction(new ParameterCases<R>() {
      @Override
      public R regularParameter(AbstractRegularParameter parameter) {
        return regularParameter.apply(parameter);
      }
      @Override
      public R beanParameter(AbstractBeanParameter parameter) {
        return beanParameter.apply(parameter);
      }
    });
  }

  public abstract static class AbstractParameter {

    /**
     * <p>for beans, this is the type that's returned by the getter,
     * or equivalently the type of the setter parameter</p>
     * <p>for regular goals, it is the original parameter type</p>
     */
    public final TypeName type;

    /**
     * true if null checks should be added
     */
    public final NullPolicy nullPolicy;

    /**
     * For regular parameters, this is just the parameter name.
     * For bean goals, it's the truncated, lower case getter name.
     *
     * @return parameter name
     */
    public abstract String name();

    AbstractParameter(TypeName type, NullPolicy nullPolicy) {
      this.type = type;
      this.nullPolicy = nullPolicy;
    }

    public abstract <R> R acceptParameter(ParameterCases<R> cases);
  }

  public static final Function<AbstractParameter, String> parameterName = parameterCases(
      parameter -> parameter.name,
      parameter -> parameter.name());

  private DtoParameter() {
    throw new UnsupportedOperationException("no instances");
  }
}
