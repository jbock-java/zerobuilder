package isobuilder.examples.kompliziert;

import org.junit.Test;

import static org.hamcrest.core.Is.is;
import static org.junit.Assert.assertThat;

public class BobBuilderTest {

  @Test
  public void kevinIsBob() {
    Bob bob = Bob.builder()
        .kevin("kevin")
        .chantal("chantal")
        .justin("justin")
        .updateKevin("bob")
        .build();
    assertThat(bob.kevin(), is("bob"));
    assertThat(bob.chantal(), is("chantal"));
    assertThat(bob.justin(), is("justin"));
  }

}