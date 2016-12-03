package net.zerobuilder.examples.generics;

import net.zerobuilder.Builder;
import net.zerobuilder.GoalName;
import net.zerobuilder.RejectNull;
import net.zerobuilder.Step;

import java.util.AbstractMap.SimpleEntry;
import java.util.Map;

public class TestStep {

  @Builder
  @GoalName("entry")
  static <K extends String, V extends Integer> Map.Entry<K, V> entry(@RejectNull K key,
                                                                     int suffix,
                                                                     @Step(0) V value) {
    return new SimpleEntry(key + Integer.toString(suffix), value);
  }
}
