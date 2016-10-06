package net.zerobuilder.compiler.generate;

import com.google.common.base.Optional;
import com.google.common.collect.ImmutableList;
import com.squareup.javapoet.ClassName;
import com.squareup.javapoet.ParameterSpec;
import com.squareup.javapoet.TypeName;
import net.zerobuilder.compiler.Utilities;
import net.zerobuilder.compiler.generate.DtoParameter.AbstractParameter;

import javax.lang.model.element.ExecutableElement;

import static com.google.common.collect.Iterables.getOnlyElement;
import static com.squareup.javapoet.ClassName.OBJECT;
import static net.zerobuilder.compiler.Utilities.distinctFrom;
import static net.zerobuilder.compiler.Utilities.downcase;
import static net.zerobuilder.compiler.Utilities.parameterSpec;

public final class DtoBeanParameter {

  public static abstract class AbstractBeanParameter extends AbstractParameter {

    /**
     * Name of the getter method (could start with {@code "is"})
     */
    final String getter;

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

  interface BeanParameterCases<R> {
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
     * {@code String}
     */
    private final ParameterSpec iterationVar;
    TypeName iterationType() {
      return iterationVar.type;
    }

    /**
     * A helper method to avoid conflicting variable name.
     *
     * @param avoid a variable name
     * @return a variable that's different from {@code avoid}, preferably {@link #iterationVar}
     */
    ParameterSpec iterationVar(ParameterSpec avoid) {
      if (!iterationVar.name.equals(avoid.name)) {
        return iterationVar;
      }
      return parameterSpec(iterationVar.type, distinctFrom(iterationVar.name, avoid.name));
    }

    private LoneGetter(TypeName type, String getter, boolean nonNull, ParameterSpec iterationVar) {
      super(type, getter, nonNull);
      this.iterationVar = iterationVar;
    }

    public static LoneGetter create(TypeName type, String getter, boolean nonNull) {
      ClassName collectionType = collectionType(type);
      String name = downcase(collectionType.simpleName().toString());
      ParameterSpec iterationVar = parameterSpec(collectionType, name);
      return new LoneGetter(type, getter, nonNull, iterationVar);
    }

    private static ClassName collectionType(TypeName typeName) {
      ImmutableList<TypeName> typeArguments = Utilities.typeArguments(typeName);
      if (typeArguments.isEmpty()) {
        // raw collection
        return OBJECT;
      } else if (typeArguments.size() == 1) {
        // one type parameter
        Optional<ClassName> collectionType = Utilities.rawClassName(getOnlyElement(typeArguments));
        if (!collectionType.isPresent())
          throw new IllegalStateException("collectionType absent");
        return collectionType.get();
      } else {
        // unlikely: subclass of Collection should not have more than one type parameter
        throw new IllegalStateException("unknown collection type");
      }
    }

    @Override
    public <R> R accept(BeanParameterCases<R> cases) {
      return cases.loneGetter(this);
    }
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
