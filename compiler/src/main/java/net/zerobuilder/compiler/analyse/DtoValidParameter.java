package net.zerobuilder.compiler.analyse;

import com.google.common.base.Optional;
import com.squareup.javapoet.TypeName;
import net.zerobuilder.compiler.analyse.DtoBeanParameter.ValidBeanParameter;

import static net.zerobuilder.compiler.analyse.DtoBeanParameter.beanStepName;

public final class DtoValidParameter {

  public interface ParameterCases<R> {
    R regularParameter(ValidRegularParameter parameter);
    R beanParameter(ValidBeanParameter parameter);
  }

  public abstract static class ValidParameter {

    /**
     * <p>for beans, this is the type that's returned by the getter</p>
     * <p>for regular goals, it is the original parameter type</p>
     */
    public final TypeName type;

    /**
     * true if null checks should be added
     */
    public final boolean nonNull;

    ValidParameter(TypeName type, boolean nonNull) {
      this.type = type;
      this.nonNull = nonNull;
    }

    public abstract <R> R acceptParameter(ParameterCases<R> cases);
  }

  public static final class ValidRegularParameter extends ValidParameter {

    /**
     * <p>original parameter name</p>
     * <p>if {@link #getter} is absent, then there is also a field with this name</p>
     */
    public final String name;

    /**
     * method name; absent iff {@code toBuilder == false} or field access
     */
    public final Optional<String> getter;
    ValidRegularParameter(String name, TypeName type, Optional<String> getter, boolean nonNull) {
      super(type, nonNull);
      this.getter = getter;
      this.name = name;
    }

    @Override
    public <R> R acceptParameter(ParameterCases<R> cases) {
      return cases.regularParameter(this);
    }
  }

  public static final DtoValidParameter.ParameterCases<String> parameterName
      = new DtoValidParameter.ParameterCases<String>() {
    @Override
    public String regularParameter(DtoValidParameter.ValidRegularParameter parameter) {
      return parameter.name;
    }
    @Override
    public String beanParameter(ValidBeanParameter parameter) {
      return parameter.accept(beanStepName);
    }
  };

  private DtoValidParameter() {
    throw new UnsupportedOperationException("no instances");
  }
}
