package net.zerobuilder.examples.generics;

import net.zerobuilder.Builder;
import net.zerobuilder.GoalName;

import java.util.AbstractMap;
import java.util.Map;

public class MapEntry {

  @Builder
  @GoalName("entry")
  static <K, V> Map.Entry<K, V> entry(K key, V value) {
    return new AbstractMap.SimpleEntry(key, value);
  }
}
