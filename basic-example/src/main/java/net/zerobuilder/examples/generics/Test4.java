package net.zerobuilder.examples.generics;

import net.zerobuilder.Builder;
import net.zerobuilder.GoalName;

import java.util.AbstractMap;
import java.util.List;
import java.util.Map;

class Test4 {

  @Builder
  @GoalName("entry")
  static <K extends String, V extends Integer> Map.Entry<K, V> entry(K key, V value, List<K> ks, V value2) {
    return new AbstractMap.SimpleEntry(key + String.join("", ks),
        Integer.valueOf(value) + Integer.valueOf(value2));
  }
}
