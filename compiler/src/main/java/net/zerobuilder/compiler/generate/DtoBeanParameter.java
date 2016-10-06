package net.zerobuilder.compiler.generate;

import com.squareup.javapoet.ClassName;
import com.squareup.javapoet.ParameterSpec;
import com.squareup.javapoet.TypeName;
import net.zerobuilder.compiler.generate.DtoParameter.AbstractParameter;

import javax.lang.model.element.ExecutableElement;
import javax.lang.model.type.TypeMirror;

import static com.google.auto.common.MoreTypes.asTypeElement;
import static net.zerobuilder.compiler.Utilities.ClassNames.OBJECT;
import static net.zerobuilder.compiler.Utilities.distinctFrom;
import static net.zerobuilder.compiler.Utilities.downcase;
import static net.zerobuilder.compiler.Utilities.parameterSpec;

public final class DtoBeanParameter {

  public static abstract class AbstractBeanParameter extends AbstractParameter {

    /**
     * Name of the getter method (could start with {@code "is"})
     */
    public final String getter;

    AbstractBeanParameter(TypeName type, String getter, boolean nonNull) {
      super(type, nonNull);
      this.getter = getter;
    }

    public abstract <R> R accept(BeanParameterCases<R> cases);
    @Override
    public final <R> R acceptParameter(DtoParameter.ParameterCases<R> cases) {
      return cases.beanParameter(this);
    }
  }

  public interface BeanParameterCases<R> {
    R accessorPair(AccessorPair pair);
    R loneGetter(LoneGetter getter);
  }

  public static final class AccessorPair extends AbstractBeanParameter {

    private AccessorPair(TypeName type, String getter, boolean nonNull) {
      super(type, getter, nonNull);
    }
    public static AccessorPair create(TypeName type, ExecutableElement getter, boolean nonNull) {
      return new AccessorPair(type, getter.getSimpleName().toString(), nonNull);
    }
    @Override
    public <R> R accept(BeanParameterCases<R> cases) {
      return cases.accessorPair(this);
    }
  }

  public static final class LoneGetter extends AbstractBeanParameter {

    /**
     * Example: If getter returns {@code List<String>}, then this would be a variable of type
     * {@ccode String}
     */
    private final ParameterSpec iterationVar;
    public TypeName iterationType() {
      return iterationVar.type;
    }

    /**
     * avoid clashing variable names
     *
     * @param avoid a variable name that is already taken
     * @return a variable that's different from {@code avoid}
     */
    public ParameterSpec iterationVar(ParameterSpec avoid) {
      if (!iterationVar.name.equals(avoid.name)) {
        return iterationVar;
      }
      return parameterSpec(iterationVar.type, distinctFrom(iterationVar.name, avoid.name));
    }

    private LoneGetter(TypeName type, String getter, boolean nonNull, ParameterSpec iterationVar) {
      super(type, getter, nonNull);
      this.iterationVar = iterationVar;
    }
    @Override
    public <R> R accept(BeanParameterCases<R> cases) {
      return cases.loneGetter(this);
    }
  }

  public static final class LoneGetterBuilder {
    private final ParameterSpec iterationVar;
    LoneGetterBuilder(ParameterSpec iterationVar) {
      this.iterationVar = iterationVar;
    }
    public LoneGetter build(TypeName type, String getter, boolean nonNull) {
      return new LoneGetter(type, getter, nonNull, iterationVar);
    }
  }

  public static LoneGetterBuilder builder(TypeMirror type) {
    TypeName typeName = TypeName.get(type);
    String name = downcase(ClassName.get(asTypeElement(type)).simpleName().toString());
    ParameterSpec iterationVar = parameterSpec(typeName, name);
    return new LoneGetterBuilder(iterationVar);
  }

  public static LoneGetterBuilder builder() {
    return new LoneGetterBuilder(parameterSpec(OBJECT, "object"));
  }

  public static final DtoBeanParameter.BeanParameterCases<String> beanParameterName
      = new BeanParameterCases<String>() {
    @Override
    public String accessorPair(AccessorPair pair) {
      return parameterName(pair);
    }
    @Override
    public String loneGetter(LoneGetter getter) {
      return parameterName(getter);
    }
  };

  private static String parameterName(AbstractBeanParameter parameter) {
    return downcase(parameter.getter.substring(parameter.getter.startsWith("is") ? 2 : 3));
  }

  private DtoBeanParameter() {
    throw new UnsupportedOperationException("no instances");
  }
}
