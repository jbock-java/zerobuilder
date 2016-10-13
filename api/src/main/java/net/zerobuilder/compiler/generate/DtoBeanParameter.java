package net.zerobuilder.compiler.generate;

import com.squareup.javapoet.ClassName;
import com.squareup.javapoet.ParameterSpec;
import com.squareup.javapoet.TypeName;
import net.zerobuilder.NullPolicy;
import net.zerobuilder.compiler.generate.DtoParameter.AbstractParameter;

import java.util.List;
import java.util.function.Supplier;

import static com.squareup.javapoet.ClassName.OBJECT;
import static net.zerobuilder.compiler.generate.Utilities.distinctFrom;
import static net.zerobuilder.compiler.generate.Utilities.downcase;
import static net.zerobuilder.compiler.generate.Utilities.memoize;
import static net.zerobuilder.compiler.generate.Utilities.onlyTypeArgument;
import static net.zerobuilder.compiler.generate.Utilities.parameterSpec;
import static net.zerobuilder.compiler.generate.Utilities.rawClassName;
import static net.zerobuilder.compiler.generate.Utilities.upcase;

public final class DtoBeanParameter {

  public static abstract class AbstractBeanParameter extends AbstractParameter {

    /**
     * Name of the getter method (could start with {@code "is"})
     */
    final String getter;

    final List<TypeName> getterThrownTypes;

    private final Supplier<String> name;

    private AbstractBeanParameter(TypeName type, String getter, NullPolicy nullPolicy, List<TypeName> getterThrownTypes) {
      super(type, nullPolicy);
      this.getter = getter;
      this.getterThrownTypes = getterThrownTypes;
      this.name = memoizeName(getter);
    }

    private static Supplier<String> memoizeName(String getter) {
      return memoize(() ->
          downcase(getter.substring(getter.startsWith("is") ? 2 : 3)));
    }

    @Override
    public final String name() {
      return name.get();
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

    private final Supplier<String> setterName;

    private AccessorPair(TypeName type, String getter, NullPolicy nullPolicy,
                         List<TypeName> getterThrownTypes, List<TypeName> setterThrownTypes) {
      super(type, getter, nullPolicy, getterThrownTypes);
      this.setterThrownTypes = setterThrownTypes;
      this.setterName = memoizeSetterName(name());
    }

    private static Supplier<String> memoizeSetterName(String name) {
      return memoize(() -> "set" + upcase(name));
    }

    String setterName() {
      return setterName.get();
    }

    @Override
    public <R> R accept(BeanParameterCases<R> cases) {
      return cases.accessorPair(this);
    }
  }

  static final class LoneGetter extends AbstractBeanParameter {

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

    @Override
    public <R> R accept(BeanParameterCases<R> cases) {
      return cases.loneGetter(this);
    }
  }

  /**
   * Creates a parameter object that describes a standard accessor pair.
   *
   * @param type              the type returned by the getter
   * @param getter            getter name
   * @param nullPolicy        null policy
   * @param getterThrownTypes thrown types
   * @param setterThrownTypes thrown types
   * @return accessor pair
   */
  public static AbstractBeanParameter accessorPair(TypeName type, String getter, NullPolicy nullPolicy,
                                                   List<TypeName> getterThrownTypes, List<TypeName> setterThrownTypes) {
    return new AccessorPair(type, getter, nullPolicy,
        getterThrownTypes, setterThrownTypes);
  }

  /**
   * Creates a parameter object that describes a lone getter accessor.
   *
   * @param type              should be a subclass of {@link java.util.Collection}
   * @param getter            getter name
   * @param nullPolicy        null policy
   * @param getterThrownTypes thrown types
   * @return lone getter
   * @throws IllegalArgumentException if {@code type} has more than one type parameter
   */
  public static AbstractBeanParameter loneGetter(TypeName type, String getter, NullPolicy nullPolicy,
                                                 List<TypeName> getterThrownTypes) {
    TypeName collectionType = onlyTypeArgument(type).orElse(OBJECT);
    String name = rawClassName(collectionType)
        .map(ClassName::simpleName)
        .map(Utilities::downcase)
        .orElseThrow(IllegalStateException::new);
    ParameterSpec iterationVar = parameterSpec(collectionType, name);
    return new LoneGetter(type, getter, nullPolicy, iterationVar, getterThrownTypes);
  }

  private DtoBeanParameter() {
    throw new UnsupportedOperationException("no instances");
  }
}
