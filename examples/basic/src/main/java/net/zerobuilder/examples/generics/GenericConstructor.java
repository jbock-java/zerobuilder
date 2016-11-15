package net.zerobuilder.examples.generics;

import net.zerobuilder.Builders;
import net.zerobuilder.Goal;

import java.util.AbstractMap;
import java.util.Map;

@Builders
final class GenericConstructor<S extends String, T extends S> {

  private final Map.Entry<S, T> entry;

  GenericConstructor(Map.Entry<S, T> entry) {
    this.entry = entry;
  }

  @Goal
  static <S extends String, T extends S> GenericConstructor<S, T> create(S ess, T tee) {
    return new GenericConstructor(new AbstractMap.SimpleEntry(ess, tee));
  }

}
