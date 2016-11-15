package net.zerobuilder.examples.generics;

import net.zerobuilder.Builders;
import net.zerobuilder.Goal;

import java.util.AbstractMap;

@Builders
final class GenericConstructor<K, V> extends AbstractMap.SimpleEntry<K, V> {

  @Goal
  GenericConstructor(K key, V value) {
    super(key, value);
  }
}
