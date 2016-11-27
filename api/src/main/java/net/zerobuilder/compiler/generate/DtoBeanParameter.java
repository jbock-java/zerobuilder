package net.zerobuilder.compiler.generate;

import com.squareup.javapoet.ClassName;
import com.squareup.javapoet.ParameterSpec;
import com.squareup.javapoet.TypeName;
import net.zerobuilder.NullPolicy;
import net.zerobuilder.compiler.generate.DtoParameter.AbstractParameter;

import java.util.Collections;
import java.util.List;
import java.util.function.Function;

import static com.squareup.javapoet.ClassName.OBJECT;
import static net.zerobuilder.compiler.generate.ZeroUtil.distinctFrom;
import static net.zerobuilder.compiler.generate.ZeroUtil.downcase;
import static net.zerobuilder.compiler.generate.ZeroUtil.onlyTypeArgument;
import static net.zerobuilder.compiler.generate.ZeroUtil.parameterSpec;
import static net.zerobuilder.compiler.generate.ZeroUtil.rawClassName;
import static net.zerobuilder.compiler.generate.ZeroUtil.upcase;

public final class DtoBeanParameter {

  public interface BeanParameterCases<R> {
    R accessorPair(AccessorPair pair);
    R loneGetter(LoneGetter getter);
  }

  public static <R> Function<AbstractBeanParameter, R> asFunction(BeanParameterCases<R> cases) {
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


  public static abstract class AbstractBeanParameter extends AbstractParameter {

    /**
     * Name of the getter method (could start with {@code "is"})
     */
    public final String getter;

    public final List<TypeName> getterThrownTypes;

    private final String name;

    private AbstractBeanParameter(TypeName type, String getter, NullPolicy nullPolicy, List<TypeName> getterThrownTypes) {
      super(type, nullPolicy);
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

    private AccessorPair(TypeName type, String getter, NullPolicy nullPolicy,
                         List<TypeName> getterThrownTypes, List<TypeName> setterThrownTypes) {
      super(type, getter, nullPolicy, getterThrownTypes);
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
        .map(ZeroUtil::downcase)
        .orElseThrow(IllegalStateException::new);
    ParameterSpec iterationVar = parameterSpec(collectionType, name);
    return new LoneGetter(type, getter, nullPolicy, iterationVar, getterThrownTypes);
  }

  private DtoBeanParameter() {
    throw new UnsupportedOperationException("no instances");
  }
}
