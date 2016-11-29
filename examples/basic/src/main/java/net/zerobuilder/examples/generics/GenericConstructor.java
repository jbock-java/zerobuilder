package net.zerobuilder.examples.generics;

import net.zerobuilder.Goal;

import java.io.IOException;
import java.util.AbstractMap;

final class GenericConstructor<K, V> extends AbstractMap.SimpleEntry<K, V> {

  @Goal(updater = true)
  GenericConstructor(K key, V value) throws IOException {
    super(key, value);
  }
}
