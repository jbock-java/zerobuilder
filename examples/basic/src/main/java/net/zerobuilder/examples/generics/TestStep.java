package net.zerobuilder.examples.generics;

import net.zerobuilder.Goal;
import net.zerobuilder.Step;

import java.util.AbstractMap.SimpleEntry;
import java.util.Map;

import static net.zerobuilder.compiler.generate.NullPolicy.REJECT;

public class TestStep {

  @Goal(name = "entry")
  static <K extends String, V extends Integer> Map.Entry<K, V> entry(@Step(nullPolicy = REJECT) K key,
                                                                     int suffix,
                                                                     @Step(0) V value) {
    return new SimpleEntry(key + Integer.toString(suffix), value);
  }
}
