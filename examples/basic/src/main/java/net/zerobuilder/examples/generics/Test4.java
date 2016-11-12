package net.zerobuilder.examples.generics;

import net.zerobuilder.Builders;
import net.zerobuilder.Goal;

import java.util.AbstractMap;
import java.util.List;
import java.util.Map;

@Builders
public class Test4 {

  @Goal(name = "entry")
  static <K extends String, V extends Integer> Map.Entry<K, V> entry(K key, V value, List<K> ks, V value2) {
    return new AbstractMap.SimpleEntry(key + String.join("", ks),
        Integer.valueOf(value) + Integer.valueOf(value2));
  }
}
