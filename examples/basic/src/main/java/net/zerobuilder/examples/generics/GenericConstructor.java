package net.zerobuilder.examples.generics;

import net.zerobuilder.Builder;
import net.zerobuilder.Updater;

import java.io.IOException;
import java.util.AbstractMap;

final class GenericConstructor<K, V> extends AbstractMap.SimpleEntry<K, V> {

  @Builder
  GenericConstructor(K key, V value) throws IOException {
    super(key, value);
  }
}
