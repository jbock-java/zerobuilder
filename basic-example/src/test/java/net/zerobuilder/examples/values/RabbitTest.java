package net.zerobuilder.examples.values;

import org.junit.jupiter.api.Test;

import static net.zerobuilder.examples.values.RabbitBuilders.hareBuilder;
import static net.zerobuilder.examples.values.RabbitBuilders.hareUpdater;
import static org.junit.jupiter.api.Assertions.assertEquals;

class RabbitTest {

  @Test
  void testHare() {
    Rabbit hare = hareBuilder().name("Roger");
    assertEquals("Roger", hare.name);
    hare = hareUpdater(hare).name("Ralph").build();
    assertEquals("Ralph", hare.name);
  }
}
