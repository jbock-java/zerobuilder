package isobuilder.examples.kompliziert;

import org.junit.Test;

import static org.hamcrest.core.Is.is;
import static org.junit.Assert.assertThat;

public class BobTest {

  @Test
  public void kevinIsBob() {
    Bob bob = BobBuilder
        .kevin("kevin")
        .chantal("chantal")
        .justin("justin")
        .updateKevin("bob")
        .build();
    assertThat(bob, is(Bob.create("bob", "chantal", "justin")));
  }

}