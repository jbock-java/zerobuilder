package net.zerobuilder.examples.generics;

import net.zerobuilder.Builders;
import net.zerobuilder.Goal;

import java.util.AbstractMap;
import java.util.Map;

@Builders
final class GenericConstructor<S extends String, T extends S> {

  private final Map.Entry<S, T> entry;

  // TODO make it work
//  @Goal
  GenericConstructor(S ess, T tee) {
    entry = new AbstractMap.SimpleEntry(ess, tee);
  }

}
