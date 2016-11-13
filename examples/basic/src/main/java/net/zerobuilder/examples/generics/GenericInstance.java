package net.zerobuilder.examples.generics;

import net.zerobuilder.Builders;
import net.zerobuilder.Goal;

import java.util.AbstractMap;
import java.util.Map;

@Builders
final class GenericInstance<S extends String> {

  // TODO make it work
//  @Goal(name = "entry")
  <K, V> Map.Entry<K, V> entry(S suffix, K key, V value) {
    return new AbstractMap.SimpleEntry(key, value + String.valueOf(suffix));
  }
}
