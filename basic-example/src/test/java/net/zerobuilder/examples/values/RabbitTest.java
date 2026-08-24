package net.zerobuilder.examples.values;

import org.junit.jupiter.api.Test;

import static net.zerobuilder.examples.values.RabbitBuilders.builder;
import static org.junit.jupiter.api.Assertions.assertEquals;

class RabbitTest {

  @Test
  void testHare() {
    Rabbit hare = builder().name("Roger");
    assertEquals("Roger", hare.name);
    hare = builder(hare).name("Ralph").build();
    assertEquals("Ralph", hare.name);
  }
}
