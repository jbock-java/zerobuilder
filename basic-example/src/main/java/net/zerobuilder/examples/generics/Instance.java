package net.zerobuilder.examples.generics;

import net.zerobuilder.Builder;
import net.zerobuilder.GoalName;

import java.util.AbstractMap;
import java.util.Map;

final class Instance<S extends String> {

  private final S prefix;

  Instance(S prefix) {
    this.prefix = prefix;
  }

  @Builder
  @GoalName("entry")
  <K, V extends S> Map.Entry<K, V> entry(K key, V value) {
    return new AbstractMap.SimpleEntry(key, String.valueOf(prefix) + value);
  }

  @Builder
  @GoalName("ventry")
  <V extends S> Map.Entry<V, V> ventry(V value) {
    return new AbstractMap.SimpleEntry(value, value);
  }
}
