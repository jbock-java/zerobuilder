package net.zerobuilder.examples.generics;

import net.zerobuilder.RecordBuilder;

@RecordBuilder
final class GenericConstructor<K, V> {
  K key;
  V value;

  GenericConstructor(K key, V value) {
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
