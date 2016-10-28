package net.zerobuilder;

public enum NullPolicy {

  ALLOW_NULL {
    @Override
    public boolean check() {
      return false;
    }
  },

  REJECT_NULL {
    @Override
    public boolean check() {
      return true;
    }
  },

  /**
   * Use inherited, or {@link #ALLOW_NULL}
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
