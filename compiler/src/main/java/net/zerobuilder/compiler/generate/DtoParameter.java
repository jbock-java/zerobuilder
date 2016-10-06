package net.zerobuilder.compiler.generate;

import com.google.common.base.Function;
import com.google.common.base.Optional;
import com.squareup.javapoet.TypeName;
import net.zerobuilder.compiler.generate.DtoBeanParameter.AbstractBeanParameter;

import javax.lang.model.element.VariableElement;

import static net.zerobuilder.compiler.generate.DtoBeanParameter.beanParameterName;

public final class DtoParameter {

  public interface ParameterCases<R> {
    R regularParameter(RegularParameter parameter);
    R beanParameter(AbstractBeanParameter parameter);
  }

  public static <R> Function<AbstractParameter, R> asFunction(final ParameterCases<R> cases) {
    return new Function<AbstractParameter, R>() {
      @Override
      public R apply(AbstractParameter parameter) {
        return parameter.acceptParameter(cases);
      }
    };
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
    public final boolean nonNull;

    public AbstractParameter(TypeName type, boolean nonNull) {
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
     * projection method name; absent iff {@code toBuilder == false} or field access
     */
    public final Optional<String> getter;

    private RegularParameter(String name, TypeName type, Optional<String> getter, boolean nonNull) {
      super(type, nonNull);
      this.getter = getter;
      this.name = name;
    }

    public static RegularParameter create(VariableElement parameter, Optional<String> getter, boolean nonNull) {
      String name = parameter.getSimpleName().toString();
      TypeName type = TypeName.get(parameter.asType());
      return new RegularParameter(name, type, getter, nonNull);
    }

    @Override
    public <R> R acceptParameter(ParameterCases<R> cases) {
      return cases.regularParameter(this);
    }
  }

  public static final Function<AbstractParameter, String> parameterName
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
