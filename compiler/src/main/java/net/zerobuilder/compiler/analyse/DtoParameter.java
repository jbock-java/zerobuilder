package net.zerobuilder.compiler.analyse;

import com.google.common.base.Optional;
import com.squareup.javapoet.TypeName;
import net.zerobuilder.compiler.analyse.DtoBeanParameter.AbstractBeanParameter;

import static net.zerobuilder.compiler.analyse.DtoBeanParameter.beanParameterName;

public final class DtoParameter {

  enum EmptyOption {
    LIST, SET, NONE
  }

  interface ParameterCases<R> {
    R regularParameter(RegularParameter parameter);
    R beanParameter(AbstractBeanParameter parameter);
  }

  public abstract static class AbstractParameter {

    /**
     * <p>for beans, this is the type that's returned by the getter</p>
     * <p>for regular goals, it is the original parameter type</p>
     */
    public final TypeName type;

    /**
     * true if null checks should be added
     */
    public final boolean nonNull;

    AbstractParameter(TypeName type, boolean nonNull) {
      this.type = type;
      this.nonNull = nonNull;
    }

    public abstract <R> R acceptParameter(ParameterCases<R> cases);
  }

  public static final class RegularParameter extends AbstractParameter {

    /**
     * <p>original parameter name</p>
     * <p>if {@link #getter} is absent, then there is also a field with this name</p>
     */
    public final String name;

    /**
     * method name; absent iff {@code toBuilder == false} or field access
     */
    public final Optional<String> getter;
    RegularParameter(String name, TypeName type, Optional<String> getter, boolean nonNull) {
      super(type, nonNull);
      this.getter = getter;
      this.name = name;
    }

    @Override
    public <R> R acceptParameter(ParameterCases<R> cases) {
      return cases.regularParameter(this);
    }
  }

  public static final DtoParameter.ParameterCases<String> parameterName
      = new DtoParameter.ParameterCases<String>() {
    @Override
    public String regularParameter(RegularParameter parameter) {
      return parameter.name;
    }
    @Override
    public String beanParameter(AbstractBeanParameter parameter) {
      return parameter.accept(beanParameterName);
    }
  };

  private DtoParameter() {
    throw new UnsupportedOperationException("no instances");
  }
}
