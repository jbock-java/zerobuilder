package net.zerobuilder.compiler.generate;

import com.squareup.javapoet.ClassName;
import com.squareup.javapoet.ParameterSpec;
import com.squareup.javapoet.TypeName;
import net.zerobuilder.NullPolicy;
import net.zerobuilder.compiler.generate.DtoParameter.AbstractParameter;

import javax.lang.model.element.ExecutableElement;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

import static com.squareup.javapoet.ClassName.OBJECT;
import static java.util.Collections.emptyList;
import static net.zerobuilder.compiler.generate.Utilities.distinctFrom;
import static net.zerobuilder.compiler.generate.Utilities.downcase;
import static net.zerobuilder.compiler.generate.Utilities.parameterSpec;
import static net.zerobuilder.compiler.generate.Utilities.rawClassName;

public final class DtoBeanParameter {

  public static abstract class AbstractBeanParameter extends AbstractParameter {

    /**
     * Name of the getter method (could start with {@code "is"})
     */
    final String getter;

    final List<TypeName> getterThrownTypes;

    AbstractBeanParameter(TypeName type, String getter, NullPolicy nullPolicy, List<TypeName> getterThrownTypes) {
      super(type, nullPolicy);
      this.getter = getter;
      this.getterThrownTypes = getterThrownTypes;
    }

    public String name() {
      return downcase(getter.substring(getter.startsWith("is") ? 2 : 3));
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

    final List<TypeName> setterThrownTypes;

    private AccessorPair(TypeName type, String getter, NullPolicy nullPolicy,
                         List<TypeName> getterThrownTypes, List<TypeName> setterThrownTypes) {
      super(type, getter, nullPolicy, getterThrownTypes);
      this.setterThrownTypes = setterThrownTypes;
    }

    public static AccessorPair create(TypeName type, ExecutableElement getter, NullPolicy nullPolicy,
                                      List<TypeName> getterThrownTypes, List<TypeName> setterThrownTypes) {
      return new AccessorPair(type, getter.getSimpleName().toString(), nullPolicy,
          getterThrownTypes, setterThrownTypes);
    }

    public static AccessorPair create(TypeName type, ExecutableElement getter, NullPolicy nullPolicy) {
      return new AccessorPair(type, getter.getSimpleName().toString(), nullPolicy, emptyList(), emptyList());
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

    private LoneGetter(TypeName type, String getter, NullPolicy nullPolicy, ParameterSpec iterationVar,
                       List<TypeName> getterThrownTypes) {
      super(type, getter, nullPolicy, getterThrownTypes);
      this.iterationVar = iterationVar;
    }

    public static LoneGetter create(TypeName type, String getter, NullPolicy nullPolicy,
                                    List<TypeName> getterThrownTypes) {
      ClassName collectionType = collectionType(type);
      String name = downcase(collectionType.simpleName());
      ParameterSpec iterationVar = parameterSpec(collectionType, name);
      return new LoneGetter(type, getter, nullPolicy, iterationVar, getterThrownTypes);
    }

    public static LoneGetter create(TypeName type, String getter, NullPolicy nullPolicy) {
      ClassName collectionType = collectionType(type);
      String name = downcase(collectionType.simpleName());
      ParameterSpec iterationVar = parameterSpec(collectionType, name);
      return new LoneGetter(type, getter, nullPolicy, iterationVar, emptyList());
    }

    private static ClassName collectionType(TypeName typeName) {
      List<TypeName> typeArguments = Utilities.typeArguments(typeName);
      if (typeArguments.isEmpty()) {
        // raw collection
        return OBJECT;
      } else if (typeArguments.size() == 1) {
        // one type parameter
        Optional<ClassName> collectionType = rawClassName(typeArguments.get(0));
        if (!collectionType.isPresent())
          throw new IllegalArgumentException("collectionType absent");
        return collectionType.get();
      } else {
        // unlikely: subclass of Collection should not have more than one type parameter
        throw new IllegalArgumentException("unknown collection type");
      }
    }

    @Override
    public <R> R accept(BeanParameterCases<R> cases) {
      return cases.loneGetter(this);
    }
  }

  private DtoBeanParameter() {
    throw new UnsupportedOperationException("no instances");
  }
}
