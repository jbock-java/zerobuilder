package net.zerobuilder;

import javax.lang.model.element.Modifier;
import java.util.Arrays;

import static java.util.Arrays.copyOf;

public enum Access {
  PUBLIC {
    @Override
    public Modifier[] modifiers(Modifier... modifiers) {
      return add(Modifier.PUBLIC, modifiers);
    }
  }, PACKAGE {
    @Override
    public Modifier[] modifiers(Modifier... modifiers) {
      return modifiers;
    }
  }, PRIVATE {
    @Override
    public Modifier[] modifiers(Modifier... modifiers) {
      return add(Modifier.PRIVATE, modifiers);
    }
  };

  private static Modifier[] add(Modifier modifier, Modifier[] modifiers) {
    for (Modifier m : modifiers) {
      if (m == modifier) {
        return modifiers;
      }
    }
    modifiers = copyOf(modifiers, modifiers.length + 1);
    modifiers[modifiers.length - 1] = modifier;
    return modifiers;
  }

  public abstract Modifier[] modifiers(Modifier... modifiers);
}
