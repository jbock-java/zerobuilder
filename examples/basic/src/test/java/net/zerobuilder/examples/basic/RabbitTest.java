package net.zerobuilder.examples.basic;

import org.junit.Test;

import static net.zerobuilder.examples.basic.RabbitBuilders.hareBuilder;
import static net.zerobuilder.examples.basic.RabbitBuilders.hareToBuilder;
import static org.hamcrest.core.Is.is;
import static org.junit.Assert.assertThat;

public class RabbitTest {

  @Test
  public void testHare() {
    Rabbit hare = hareBuilder().name("Roger");
    assertThat(hare.name, is("Roger"));
    hare = hareToBuilder(hare).name("Ralph").build();
    assertThat(hare.name, is("Ralph"));
  }

}