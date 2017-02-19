package net.zerobuilder.examples.generics;

import net.zerobuilder.Builder;
import net.zerobuilder.GoalName;

import java.util.AbstractMap.SimpleEntry;
import java.util.Map;

final class StaticGenericInstance<S> {

  @Builder
  @GoalName("entry")
  static <S, K, V> Map.Entry<K, V> entry(S suffix,
                                         K key,
                                         V value) {
    return new SimpleEntry(key, value + String.valueOf(suffix));
  }
}
