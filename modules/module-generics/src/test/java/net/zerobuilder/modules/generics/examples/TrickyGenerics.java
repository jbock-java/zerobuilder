package net.zerobuilder.modules.generics.examples;

import java.util.List;
import java.util.Map;

import static java.util.Collections.singletonList;

public class TrickyGenerics {

  // @Goal
  static <K, V> List<V> getList(Map<K, List<V>> source, K key, V defaultValue) {
    List<V> list = source.get(key);
    return list == null ? singletonList(defaultValue) : list;
  }
}
