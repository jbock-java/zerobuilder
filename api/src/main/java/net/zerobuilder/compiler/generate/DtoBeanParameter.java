package net.zerobuilder.compiler.generate;

import io.jbock.javapoet.ParameterSpec;
import io.jbock.javapoet.TypeName;

import java.util.Collections;
import java.util.List;
import java.util.function.Function;

import static io.jbock.javapoet.ClassName.OBJECT;
import static net.zerobuilder.compiler.generate.ZeroUtil.*;

public final class DtoBeanParameter {

  interface BeanParameterCases<R> {
    R accessorPair(AccessorPair pair);

    R loneGetter(LoneGetter getter);
  }

  private static <R> Function<AbstractBeanParameter, R> asFunction(BeanParameterCases<R> cases) {
    return parameter -> parameter.accept(cases);
  }

  public static <R> Function<AbstractBeanParameter, R> beanParameterCases(
      Function<AccessorPair, R> accessorPairFunction,
      Function<LoneGetter, R> loneGetterFunction) {
    return asFunction(new BeanParameterCases<R>() {
      @Override
      public R accessorPair(AccessorPair pair) {
        return accessorPairFunction.apply(pair);
      }

      @Override
      public R loneGetter(LoneGetter getter) {
        return loneGetterFunction.apply(getter);
      }
    });
  }

  private static final Function<AbstractBeanParameter, List<TypeName>> getterThrownTypes =
      beanParameterCases(
          accessorPair -> accessorPair.getterThrownTypes,
          loneGetter -> loneGetter.getterThrownTypes);

  private static final Function<AbstractBeanParameter, List<TypeName>> setterThrownTypes =
      beanParameterCases(
          accessorPair -> accessorPair.setterThrownTypes,
          loneGetter -> Collections.emptyList());


  public static abstract class AbstractBeanParameter {

    /**
     * the type that's returned by the getter,
     * or equivalently the type of the setter parameter
     */
    public final TypeName type;

    /**
     * Name of the getter method (could start with {@code "is"})
     */
    public final String getter;

    public final List<TypeName> getterThrownTypes;

    private final String name;

    private AbstractBeanParameter(TypeName type, String getter, List<TypeName> getterThrownTypes) {
      this.type = type;
      this.getter = getter;
      this.getterThrownTypes = getterThrownTypes;
      this.name = downcase(getter.substring(getter.startsWith("is") ? 2 : 3));
    }

    public final List<TypeName> getterThrownTypes() {
      return DtoBeanParameter.getterThrownTypes.apply(this);
    }

    public final List<TypeName> setterThrownTypes() {
      return DtoBeanParameter.setterThrownTypes.apply(this);
    }

    public final String name() {
      return name;
    }

    public abstract <R> R accept(BeanParameterCases<R> cases);
  }

  public static final class AccessorPair extends AbstractBeanParameter {

    public final List<TypeName> setterThrownTypes;

    private final String setterName;

    private AccessorPair(TypeName type, String getter,
                         List<TypeName> getterThrownTypes, List<TypeName> setterThrownTypes) {
      super(type, getter, getterThrownTypes);
      this.setterThrownTypes = setterThrownTypes;
      this.setterName = "set" + upcase(name());
    }

    public String setterName() {
      return setterName;
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

    public TypeName iterationType() {
      return iterationVar.type;
    }

    /**
     * A helper method to avoid conflicting variable name.
     *
     * @param avoid a variable name
     * @return a variable that's different from {@code avoid}, preferably {@link #iterationVar}
     */
    public ParameterSpec iterationVar(ParameterSpec avoid) {
      if (!iterationVar.name.equals(avoid.name)) {
        return iterationVar;
      }
      return parameterSpec(iterationVar.type, distinctFrom(iterationVar.name, avoid.name));
    }

    private LoneGetter(TypeName type, String getter, ParameterSpec iterationVar,
                       List<TypeName> getterThrownTypes) {
      super(type, getter, getterThrownTypes);
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
   * @param getterThrownTypes thrown types
   * @param setterThrownTypes thrown types
   * @return accessor pair
   */
  public static AbstractBeanParameter accessorPair(TypeName type, String getter,
                                                   List<TypeName> getterThrownTypes, List<TypeName> setterThrownTypes) {
    return new AccessorPair(type, getter,
        getterThrownTypes, setterThrownTypes);
  }

  /**
   * Creates a parameter object that describes a lone getter accessor.
   *
   * @param type              should be a subclass of {@link java.util.Collection}
   * @param getter            getter name
   * @param getterThrownTypes thrown types
   * @return lone getter
   * @throws IllegalArgumentException if {@code type} has more than one type parameter
   */
  public static AbstractBeanParameter loneGetter(TypeName type, String getter,
                                                 List<TypeName> getterThrownTypes) {
    TypeName collectionType = onlyTypeArgument(type).orElse(OBJECT);
    String name = downcase(simpleName(collectionType));
    ParameterSpec iterationVar = parameterSpec(collectionType, name);
    return new LoneGetter(type, getter, iterationVar, getterThrownTypes);
  }

  private DtoBeanParameter() {
    throw new UnsupportedOperationException("no instances");
  }
}
