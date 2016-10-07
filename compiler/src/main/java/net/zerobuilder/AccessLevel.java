package net.zerobuilder;

import net.zerobuilder.compiler.generate.Access;

public enum AccessLevel {

  PUBLIC {
    @Override
    public Access access() {
      return Access.PUBLIC;
    }
  },

  PACKAGE {
    @Override
    public Access access() {
      return Access.PACKAGE;
    }
  },

  /**
   * Use inherited, or {@link #PUBLIC}
   * if nothing is inherited or inherited is also {@link #UNSPECIFIED}
   */
  UNSPECIFIED {
    @Override
    public Access access() {
      throw new UnsupportedOperationException("not implemented");
    }
  };

  public abstract Access access();
}
