package net.zerobuilder.examples.generics;

import net.zerobuilder.Builders;
import net.zerobuilder.Goal;

import java.util.AbstractMap;
import java.util.List;
import java.util.Map;

@Builders
public class Test3 {

  @Goal(name = "entry")
  static <K extends String, V extends Integer> Map.Entry<K, V> entry(K key, int suffix, V value) {
    return new AbstractMap.SimpleEntry(key + Integer.toString(suffix), value);
  }
}
