package net.zerobuilder.compiler.generate;

import com.palantir.javapoet.ParameterSpec;
import com.palantir.javapoet.TypeName;

import java.util.List;

import static com.palantir.javapoet.ClassName.OBJECT;
import static net.zerobuilder.compiler.generate.ZeroUtil.distinctFrom;
import static net.zerobuilder.compiler.generate.ZeroUtil.downcase;
import static net.zerobuilder.compiler.generate.ZeroUtil.onlyTypeArgument;
import static net.zerobuilder.compiler.generate.ZeroUtil.parameterSpec;
import static net.zerobuilder.compiler.generate.ZeroUtil.simpleName;
import static net.zerobuilder.compiler.generate.ZeroUtil.upcase;

public final class DtoBeanParameter {

  public sealed interface AbstractBeanParameter permits AccessorPair, LoneGetter {

    /**
     * the type that's returned by the getter,
     * or equivalently the type of the setter parameter
     */
    TypeName type();

    /**
     * Name of the getter method (could start with {@code "is"})
     */
    String getter();

    List<TypeName> getterThrownTypes();

    List<TypeName> setterThrownTypes();

    String name();
  }

  static abstract class LessAbstractBeanParameter {

    /**
     * the type that's returned by the getter,
     * or equivalently the type of the setter parameter
     */
    public final TypeName type;

    /**
     * Name of the getter method (could start with {@code "is"})
     */
    private final String getter;

    public final List<TypeName> getterThrownTypes;

    private final String name;

    private LessAbstractBeanParameter(TypeName type, String getter, List<TypeName> getterThrownTypes) {
      this.type = type;
      this.getter = getter;
      this.getterThrownTypes = getterThrownTypes;
      this.name = downcase(getter.substring(getter.startsWith("is") ? 2 : 3));
    }

    public final TypeName type() {
      return type;
    }

    public final String getter() {
      return getter;
    }

    public final String name() {
      return name;
    }
  }

  public static final class AccessorPair extends LessAbstractBeanParameter implements AbstractBeanParameter {

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
    public List<TypeName> getterThrownTypes() {
      return getterThrownTypes;
    }

    @Override
    public List<TypeName> setterThrownTypes() {
      return setterThrownTypes;
    }
  }

  public static final class LoneGetter extends LessAbstractBeanParameter implements AbstractBeanParameter {

    /**
     * Example: If getter returns {@code List<String>}, then this would be a variable of type
     * {@code String}
     */
    private final ParameterSpec iterationVar;

    public TypeName iterationType() {
      return iterationVar.type();
    }

    /**
     * A helper method to avoid conflicting variable name.
     *
     * @param avoid a variable name
     * @return a variable that's different from {@code avoid}, preferably {@link #iterationVar}
     */
    public ParameterSpec iterationVar(ParameterSpec avoid) {
      if (!iterationVar.name().equals(avoid.name())) {
        return iterationVar;
      }
      return parameterSpec(iterationVar.type(), distinctFrom(iterationVar.name(), avoid.name()));
    }

    private LoneGetter(TypeName type, String getter, ParameterSpec iterationVar,
                       List<TypeName> getterThrownTypes) {
      super(type, getter, getterThrownTypes);
      this.iterationVar = iterationVar;
    }

    @Override
    public List<TypeName> getterThrownTypes() {
      return getterThrownTypes;
    }

    @Override
    public List<TypeName> setterThrownTypes() {
      return List.of();
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
