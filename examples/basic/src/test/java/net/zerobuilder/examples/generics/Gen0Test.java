package net.zerobuilder.examples.generics;

import org.junit.Test;

import java.util.HashMap;
import java.util.Map;

import static java.util.Collections.emptyMap;
import static net.zerobuilder.examples.generics.Gen0Builders.barBuilder;
import static net.zerobuilder.examples.generics.Gen0Builders.barUpdaterFactory;
import static org.hamcrest.core.Is.is;
import static org.junit.Assert.assertThat;

public class Gen0Test {

  private final Map<Integer, Number> m = new HashMap<Integer, Number>() {
    {
      put(5, 6L);
    }
  };

  @Test
  public void bar() throws Exception {
    Gen0<Number, Integer> gen = new Gen0<>(1L, 2f, 3d, 4);
    Gen0.Bar<Number, Integer> bar = barBuilder(gen).map0(emptyMap())
        .map1(emptyMap())
        .map2(emptyMap())
        .map3(emptyMap());
    Gen0.Bar<Number, Integer> updated = barUpdaterFactory(gen).updater(bar).map0(m).done();
    assertThat(updated.aa0, is(1L));
    assertThat(updated.aa1, is(2f));
    assertThat(updated.aa2, is(3d));
    assertThat(updated.ab0, is(4));
    assertThat(updated.map0, is(m));
    assertThat(updated.map1, is(emptyMap()));
    assertThat(updated.map2, is(emptyMap()));
    assertThat(updated.map3, is(emptyMap()));
    assertThat(bar.aa0, is(1L));
    assertThat(bar.aa1, is(2f));
    assertThat(bar.aa2, is(3d));
    assertThat(bar.ab0, is(4));
    assertThat(bar.map0, is(emptyMap()));
    assertThat(bar.map1, is(emptyMap()));
    assertThat(bar.map2, is(emptyMap()));
    assertThat(bar.map3, is(emptyMap()));
  }
}