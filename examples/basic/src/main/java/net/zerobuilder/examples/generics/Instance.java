package net.zerobuilder.examples.generics;

import net.zerobuilder.Builders;
import net.zerobuilder.Goal;

import java.util.AbstractMap;
import java.util.Map;

@Builders
final class Instance<S extends String> {

  private final S prefix;

  Instance(S prefix) {
    this.prefix = prefix;
  }

  @Goal(name = "entry")
  <K, V extends S> Map.Entry<K, V> entry(K key, V value) {
    return new AbstractMap.SimpleEntry(key, String.valueOf(prefix) + value);
  }
}
