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

public final class DtoValidParameter {

  public abstract static class ValidParameter {

    /**
     * <p>for regular goals, this is the original parameter name</p>
     * <p>if {@code goalType == FIELD_ACCESS}, then there is also a field with this name</p>
     */
    public final String name;

    /**
     * <p>for beans, this is the type that's returned by the getter</p>
     */
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
     * method name; absent iff {@code toBuilder == false} or direct field access
     */
    public final Optional<String> getter;
    ValidRegularParameter(String name, TypeName type, Optional<String> getter, boolean nonNull) {
      super(name, type, nonNull);
      this.getter = getter;
    }
  }

  private DtoValidParameter() {
    throw new UnsupportedOperationException("no instances");
  }
}
