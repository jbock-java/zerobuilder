package net.zerobuilder.compiler.examples;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class Fuchur {

  // @Goal
  static <K, V> Map<K, V> multiKey(List<K> keys, V value) {
    Map<K, V> m = new HashMap<>();
    for (K key : keys) {
      m.put(key, value);
    }
    return m;
  }
}
