package net.zerobuilder.examples.generics;

import net.zerobuilder.Builder;
import net.zerobuilder.GoalName;

import java.util.AbstractMap;
import java.util.Map;

final class MapEntry2 {

  @Builder
  @GoalName("entry")
  static <S extends String, K, V extends S> Map.Entry<K, V> entry(K key, V value) {
    return new AbstractMap.SimpleEntry(key, value);
  }
}
