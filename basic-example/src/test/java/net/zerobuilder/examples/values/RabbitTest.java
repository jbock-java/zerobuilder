package net.zerobuilder.examples.values;

import org.junit.jupiter.api.Test;

import static net.zerobuilder.examples.values.RabbitBuilders.builder;
import static org.junit.jupiter.api.Assertions.assertEquals;

class RabbitTest {

  @Test
  void testRabbit() {
    Rabbit wabbit = builder().name("Roger");
    assertEquals("Roger", wabbit.name);
    wabbit = builder(wabbit).name("Ralph").build();
    assertEquals("Ralph", wabbit.name);
  }
}
