package net.zerobuilder.compiler.generate;

import com.squareup.javapoet.TypeName;
import net.zerobuilder.NullPolicy;

public final class DtoParameter {

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
    public final NullPolicy nullPolicy;
    
    AbstractParameter(TypeName type, NullPolicy nullPolicy) {
      this.type = type;
      this.nullPolicy = nullPolicy;
    }
  }

  private DtoParameter() {
    throw new UnsupportedOperationException("no instances");
  }
}
