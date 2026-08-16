package net.zerobuilder.modules.generics.examples;

import org.junit.jupiter.api.Test;

import java.util.Map;

import static java.util.Arrays.asList;
import static net.zerobuilder.modules.generics.examples.FuchurBuilders.multiKeyBuilder;
import static org.junit.jupiter.api.Assertions.assertEquals;

public class FuchurBuildersTest {

  @Test
  public void multi() {
    Map<String, Integer> m = multiKeyBuilder()
        .keys(asList("1", "2"))
        .value(2);
    assertEquals(2, m.size());
    assertEquals(2, m.get("1"));
    assertEquals(2, m.get("2"));
  }
}
