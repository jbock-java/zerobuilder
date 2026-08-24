package net.zerobuilder.examples.values;

import org.junit.jupiter.api.Test;

import static net.zerobuilder.examples.values.GettersBuilders.builder;
import static org.junit.jupiter.api.Assertions.assertEquals;

class GettersTest {

  @Test
  void basicTest() {
    Getters getters = builder().length(12).width(10).height(11);
    assertEquals(12d, getters.getLength());
    assertEquals(10d, getters.getWidth());
    assertEquals(11d, getters.getHeight());
    getters = builder(getters).length(0).build();
    assertEquals(0d, getters.getLength());
    assertEquals(10d, getters.getWidth());
    assertEquals(11d, getters.getHeight());
  }
}
