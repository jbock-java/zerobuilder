package net.zerobuilder.compiler.generate;

public enum NullPolicy {

  ALLOW {
    @Override
    public boolean check() {
      return false;
    }
  },

  REJECT {
    @Override
    public boolean check() {
      return true;
    }
  },

  /**
   * Use inherited, or {@link #ALLOW}
   * if nothing is inherited or inherited is also {@link #DEFAULT}
   */
  DEFAULT {
    @Override
    public boolean check() {
      throw new UnsupportedOperationException("not implemented");
    }
  };

  public abstract boolean check();
}
