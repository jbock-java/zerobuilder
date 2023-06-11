package net.zerobuilder.examples.values;

import org.junit.jupiter.api.Test;

import static net.zerobuilder.examples.values.RabbitBuilders.hareBuilder;
import static net.zerobuilder.examples.values.RabbitBuilders.hareUpdater;
import static org.junit.jupiter.api.Assertions.assertEquals;

public class RabbitTest {

  @Test
  public void testHare() {
    Rabbit hare = hareBuilder().name("Roger");
    assertEquals("Roger", hare.name);
    hare = hareUpdater(hare).name("Ralph").done();
    assertEquals("Ralph", hare.name);
  }
}
