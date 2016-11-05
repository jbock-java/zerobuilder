package net.zerobuilder.compiler.examples;

import java.util.List;
import java.util.Map;

import static java.util.Collections.singletonList;

public class TrickyGenerics {

  // @Goal
  static <K, V> List<V> getList(Map<K, List<V>> map, K key, V defaultValue) {
    List<V> list = map.get(key);
    return list == null ? singletonList(defaultValue) : list;
  }
}
