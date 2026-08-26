package net.zerobuilder.examples.generics;

import java.io.IOException;
import java.util.AbstractMap;
import net.zerobuilder.RecordBuilder;

@RecordBuilder
final class GenericConstructor<K, V> extends AbstractMap.SimpleEntry<K, V> {

  GenericConstructor(K key, V value) throws IOException {
    super(key, value);
  }
}
