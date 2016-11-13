package net.zerobuilder.examples.generics;

import net.zerobuilder.Builders;
import net.zerobuilder.Goal;
import net.zerobuilder.Step;

import java.util.AbstractMap;
import java.util.Map;

@Builders
public class TestStep {

  @Goal(name = "entry")
  static <K extends String, V extends Integer> Map.Entry<K, V> entry(K key,
                                                                     int suffix,
                                                                     @Step(0) V value) {
    return new AbstractMap.SimpleEntry(key + Integer.toString(suffix), value);
  }
}
