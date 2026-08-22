package net.zerobuilder.examples.generics;

import java.io.IOException;
import java.util.AbstractMap;
import net.zerobuilder.Builder;

final class GenericConstructor<K, V> extends AbstractMap.SimpleEntry<K, V> {

  @Builder
  GenericConstructor(K key, V value) throws IOException {
    super(key, value);
  }
}
