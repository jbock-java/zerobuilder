package isobuilder.examples.kompliziert;

import org.junit.Test;

import static org.hamcrest.core.Is.is;
import static org.junit.Assert.assertThat;

public class BobTest {

  @Test
  public void kevinIsBob() {
    Bob bob = BobBuilder.builder()
        .kevin("kevin")
        .chantal("chantal")
        .justin("justin");
    assertThat(bob, is(Bob.create("kevin", "chantal", "justin")));
    assertThat(BobBuilder.toBuilder(bob).build(),
        is(Bob.create("kevin", "chantal", "justin")));
    assertThat(BobBuilder.toBuilder(bob).kevin("bob").build(),
        is(Bob.create("bob", "chantal", "justin")));
    assertThat(BobBuilder.toBuilder(bob).chantal("bob").build(),
        is(Bob.create("kevin", "bob", "justin")));
    assertThat(BobBuilder.toBuilder(bob).justin("bob").build(),
        is(Bob.create("kevin", "chantal", "bob")));
    assertThat(BobBuilder.toBuilder(bob).kevin("bob").chantal("bob").build(),
        is(Bob.create("bob", "bob", "justin")));
    assertThat(BobBuilder.toBuilder(bob).kevin("bob").justin("bob").build(),
        is(Bob.create("bob", "chantal", "bob")));
    assertThat(BobBuilder.toBuilder(bob).chantal("bob").justin("bob").build(),
        is(Bob.create("kevin", "bob", "bob")));
    assertThat(BobBuilder.toBuilder(bob).kevin("bob").chantal("bob").justin("bob").build(),
        is(Bob.create("bob", "bob", "bob")));
  }

}