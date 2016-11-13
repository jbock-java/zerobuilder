package net.zerobuilder.examples.generics;

import net.zerobuilder.Builders;
import net.zerobuilder.Goal;
import net.zerobuilder.NullPolicy;
import net.zerobuilder.Step;

import java.util.AbstractMap;
import java.util.AbstractMap.SimpleEntry;
import java.util.Map;

import static net.zerobuilder.NullPolicy.REJECT;

@Builders
public class TestStep {

  @Goal(name = "entry")
  static <K extends String, V extends Integer> Map.Entry<K, V> entry(@Step(nullPolicy = REJECT) K key,
                                                                     int suffix,
                                                                     @Step(0) V value) {
    return new SimpleEntry(key + Integer.toString(suffix), value);
  }
}
