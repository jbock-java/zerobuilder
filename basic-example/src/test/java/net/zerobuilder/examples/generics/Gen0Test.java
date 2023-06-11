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
    Gen0.Bar<Number, Integer> bar = barBuilder(gen).map0(emptyMap())
        .map1(emptyMap())
        .map2(emptyMap())
        .map3(emptyMap());
    Gen0.Bar<Number, Integer> updated = barUpdaterFactory(gen).updater(bar).map0(m).done();
    assertEquals(updated.aa0, 1L);
    assertEquals(updated.aa1, 2f);
    assertEquals(updated.aa2, 3d);
    assertEquals(updated.ab0, 4);
    assertEquals(updated.map0, m);
    assertEquals(updated.map1, emptyMap());
    assertEquals(updated.map2, emptyMap());
    assertEquals(updated.map3, emptyMap());
    assertEquals(bar.aa0, 1L);
    assertEquals(bar.aa1, 2f);
    assertEquals(bar.aa2, 3d);
    assertEquals(bar.ab0, 4);
    assertEquals(bar.map0, emptyMap());
    assertEquals(bar.map1, emptyMap());
    assertEquals(bar.map2, emptyMap());
    assertEquals(bar.map3, emptyMap());
  }
}
