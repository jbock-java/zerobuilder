package isobuilder.examples.kompliziert;

import org.junit.Test;

import static org.hamcrest.core.Is.is;
import static org.junit.Assert.assertThat;

public class BobTest {

  @Test
  public void kevinIsBob() {
    BobBuilder.Contract.BobUpdater updater = BobBuilder
        .kevin("kevin")
        .chantal("chantal")
        .justin("justin");
    assertThat(updater.build(), is(Bob.create("bob", "chantal", "justin")));
    assertThat(updater.updateKevin("bob").build(), is(Bob.create("bob", "chantal", "justin")));
  }

}