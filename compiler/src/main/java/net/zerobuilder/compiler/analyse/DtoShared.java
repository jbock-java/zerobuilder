package net.zerobuilder.compiler.analyse;

import com.google.common.base.Optional;
import com.squareup.javapoet.ClassName;
import com.squareup.javapoet.ParameterSpec;
import com.squareup.javapoet.TypeName;

import javax.lang.model.type.TypeMirror;

import static com.google.auto.common.MoreTypes.asTypeElement;
import static com.google.common.base.Preconditions.checkState;
import static net.zerobuilder.compiler.Utilities.distinctFrom;
import static net.zerobuilder.compiler.Utilities.downcase;
import static net.zerobuilder.compiler.Utilities.parameterSpec;

public final class DtoShared {

  public abstract static class ValidParameter {

    /**
     * <p>for regular goals, this is the original parameter name</p>
     * <p>if {@code goalType == FIELD_ACCESS}, then there is also a field with this name</p>
     */
    public final String name;
    public final TypeName type;
    public final boolean nonNull;

    ValidParameter(String name, TypeName type, boolean nonNull) {
      this.name = name;
      this.type = type;
      this.nonNull = nonNull;
    }
  }

  public static final class ValidRegularParameter extends ValidParameter {

    /**
     * method name; absent iff {@code toBuilder = false} or direct field access
     */
    public final Optional<String> getter;
    ValidRegularParameter(String name, TypeName type, Optional<String> getter, boolean nonNull) {
      super(name, type, nonNull);
      this.getter = getter;
    }
  }

  public static final class ValidBeanParameter extends ValidParameter {

    /**
     * Name of the getter method (could start with "is")
     */
    public final String getter;

    /**
     * Present iff this step fills a setterless collection
     */
    public final CollectionType collectionType;

    ValidBeanParameter(TypeName type, String getter, CollectionType collectionType, boolean nonNull) {
      super(name(getter), type, nonNull);
      this.getter = getter;
      this.collectionType = collectionType;
    }

    private static String name(String projectionMethodName) {
      return downcase(projectionMethodName.substring(projectionMethodName.startsWith("is") ? 2 : 3));
    }

    public static final class CollectionType {

      private final Optional<ParameterSpec> iterationVar;
      public final boolean allowShortcut;

      public boolean isPresent() {
        return iterationVar.isPresent();
      }
      public TypeName getType() {
        return iterationVar.get().type;
      }
      public ParameterSpec get(ParameterSpec avoid) {
        ParameterSpec iterationVar = this.iterationVar.get();
        if (!iterationVar.name.equals(avoid.name)) {
          return iterationVar;
        }
        return parameterSpec(iterationVar.type,
            distinctFrom(iterationVar.name, avoid.name));
      }

      private CollectionType(Optional<ParameterSpec> iterationVar, boolean allowShortcut) {
        checkState(iterationVar.isPresent() || !allowShortcut);
        this.iterationVar = iterationVar;
        this.allowShortcut = allowShortcut;
      }
      static final CollectionType ABSENT = new CollectionType(Optional.<ParameterSpec>absent(), false);
      static CollectionType of(TypeMirror type, boolean allowShortcut) {
        TypeName typeName = TypeName.get(type);
        String name = downcase(ClassName.get(asTypeElement(type)).simpleName().toString());
        ParameterSpec iterationVar = ParameterSpec.builder(typeName, name).build();
        return new CollectionType(Optional.of(iterationVar), allowShortcut);
      }
      static CollectionType of(Class clazz, boolean allowShortcut) {
        ClassName className = ClassName.get(clazz);
        ParameterSpec iterationVar = parameterSpec(className, downcase(className.simpleName().toString()));
        return new CollectionType(Optional.of(iterationVar), allowShortcut);
      }
    }
  }

  private DtoShared() {
    throw new UnsupportedOperationException("no instances");
  }
}
