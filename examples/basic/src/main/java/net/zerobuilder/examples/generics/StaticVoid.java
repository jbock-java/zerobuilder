package net.zerobuilder.examples.generics;

import net.zerobuilder.Builder;
import net.zerobuilder.GoalName;

public class StaticVoid {

  @Builder
  @GoalName("twins")
  static <K, V> void entry(K key, V value) {
    if (key.getClass().equals(value.getClass())) {
      throw new IllegalArgumentException("twins");
    }
  }
}
