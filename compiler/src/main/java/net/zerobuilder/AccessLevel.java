package net.zerobuilder;

import javax.lang.model.element.Modifier;
import java.util.Arrays;

public enum AccessLevel {
  PUBLIC {
    @Override
    public Modifier[] modifiers(Modifier... modifiers) {
      for (Modifier modifier : modifiers) {
        if (modifier == Modifier.PUBLIC) {
          return modifiers;
        }
      }
      modifiers = Arrays.copyOf(modifiers, modifiers.length + 1);
      modifiers[modifiers.length - 1] = Modifier.PUBLIC;
      return modifiers;
    }
  }, PACKAGE {
    @Override
    public Modifier[] modifiers(Modifier... modifiers) {
      return modifiers;
    }
  }, UNSPECIFIED {
    @Override
    public Modifier[] modifiers(Modifier... modifiers) {
      throw new UnsupportedOperationException("not implemented");
    }
  };
  public abstract Modifier[] modifiers(Modifier... modifiers);
}
