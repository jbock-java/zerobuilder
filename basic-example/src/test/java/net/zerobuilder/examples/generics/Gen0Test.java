package net.zerobuilder.examples.generics;

import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.Map;

import static java.util.Collections.emptyMap;
import static net.zerobuilder.examples.generics.Gen0Builders.barBuilder;
import static net.zerobuilder.examples.generics.Gen0Builders.barUpdaterFactory;
import static org.junit.jupiter.api.Assertions.assertEquals;

public class Gen0Test {

  private final Map<Integer, Number> m = new HashMap<>() {
    {
      put(5, 6L);
    }
  };

  @Test
  public void bar() {
    Gen0<Number, Integer> gen = new Gen0<>(1L, 2f, 3d, 4);
    Gen0.Bar<Number, Integer> bar = barBuilder(gen)
        .map0(Map.of())
        .map1(Map.of())
        .map2(Map.of())
        .map3(Map.of());
    Gen0.Bar<Number, Integer> updated = barUpdaterFactory(gen).updater(bar).map0(m).build();
    assertEquals(1L, updated.aa0);
    assertEquals(2f, updated.aa1);
    assertEquals(3d, updated.aa2);
    assertEquals(4, updated.ab0);
    assertEquals(m, updated.map0);
    assertEquals(Map.of(), updated.map1);
    assertEquals(Map.of(), updated.map2);
    assertEquals(Map.of(), updated.map3);
    assertEquals(1L, bar.aa0);
    assertEquals(2f, bar.aa1);
    assertEquals(3d, bar.aa2);
    assertEquals(4, bar.ab0);
    assertEquals(Map.of(), bar.map0);
    assertEquals(Map.of(), bar.map1);
    assertEquals(Map.of(), bar.map2);
    assertEquals(Map.of(), bar.map3);
  }
}
