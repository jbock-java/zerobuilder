package net.zerobuilder.examples.generics;

import net.zerobuilder.RecordBuilder;

import java.io.IOException;

@RecordBuilder
final class GenericConstructor<K, V> {
  K key;
  V value;

  GenericConstructor(K key, V value) throws IOException {
    this.key = key;
    this.value = value;
  }

  K getKey() {
    return key;
  }

  V getValue() {
    return value;
  }
}
