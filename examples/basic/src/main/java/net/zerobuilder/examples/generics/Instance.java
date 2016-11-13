package net.zerobuilder.examples.generics;

import net.zerobuilder.Builders;
import net.zerobuilder.Goal;

import java.util.AbstractMap;
import java.util.Map;

@Builders
public class Instance {

  @Goal(name = "entry")
  <K, V> Map.Entry<K, V> entry(K key, V value) {
    return new AbstractMap.SimpleEntry(key, value);
  }
}
