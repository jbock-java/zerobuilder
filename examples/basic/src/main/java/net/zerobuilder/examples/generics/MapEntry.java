package net.zerobuilder.examples.generics;

import net.zerobuilder.Goal;

import java.util.AbstractMap;
import java.util.Map;

public class MapEntry {

  @Goal(name = "entry")
  static <K, V> Map.Entry<K, V> entry(K key, V value) {
    return new AbstractMap.SimpleEntry(key, value);
  }
}
