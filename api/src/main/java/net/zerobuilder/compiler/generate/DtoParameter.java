package net.zerobuilder.compiler.generate;

import com.squareup.javapoet.TypeName;
import net.zerobuilder.compiler.generate.DtoBeanParameter.AbstractBeanParameter;

import java.util.Optional;
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
    final boolean nonNull;

    AbstractParameter(TypeName type, boolean nonNull) {
      this.type = type;
      this.nonNull = nonNull;
    }

    public abstract <R> R acceptParameter(ParameterCases<R> cases);
  }

  /**
   * Represents one of the parameters of a method or constructor.
   */
  public static final class RegularParameter extends AbstractParameter {

    /**
     * <p>original parameter name</p>
     * <p>if {@link #getter} is absent,
     * and
     * {@link net.zerobuilder.compiler.generate.DtoGoal.GoalOptions#toBuilder == true}
     * then there is also a field with this name</p>
     */
    final String name;

    /**
     * projection method name; absent iff
     * {@link net.zerobuilder.compiler.generate.DtoGoal.GoalOptions#toBuilder == false}
     * or field access
     */
    final Optional<String> getter;

    private RegularParameter(String name, TypeName type, Optional<String> getter, boolean nonNull) {
      super(type, nonNull);
      this.getter = getter;
      this.name = name;
    }

    /**
     * This method must be used to create the parameter if either
     * {@link net.zerobuilder.compiler.generate.DtoGoal.GoalOptions#toBuilder} is {@code false}
     * or the generated {@code toBuilder} method should use direct field access.
     *
     * @param name    parameter name
     * @param type    parameter type
     * @param nonNull should the generated code reject a null value?
     * @return a parameter
     */
    public static RegularParameter create(String name, TypeName type, boolean nonNull) {
      return new RegularParameter(name, type, Optional.empty(), nonNull);
    }

    /**
     * This method must be used to create the parameter if
     * {@link net.zerobuilder.compiler.generate.DtoGoal.GoalOptions#toBuilder} is {@code true}
     * and the generated {@code toBuilder} method should use
     * a projection method, such as a getter.
     *
     * @param name    parameter name
     * @param type    parameter type
     * @param nonNull should the generated code reject a null value?
     * @param getter  name of the projection method, for example {@code getFoo}
     * @return a parameter
     */
    public static RegularParameter create(String name, TypeName type, boolean nonNull, String getter) {
      return new RegularParameter(name, type, Optional.of(getter), nonNull);
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
