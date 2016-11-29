package net.zerobuilder.examples.generics;

import net.zerobuilder.Goal;

public class StaticVoid {

  @Goal(name = "twins")
  static <K, V> void entry(K key, V value) {
    if (key.getClass().equals(value.getClass())) {
      throw new IllegalArgumentException("twins");
    }
  }
}
