package net.zerobuilder.examples.autovalue;

import org.junit.Test;

import static org.hamcrest.core.Is.is;
import static org.junit.Assert.assertThat;

public class BobTest {

  @Test
  public void updateEveryCombination() {
    Bob bob = BobBuilder.builder()
        .kevin("kevin")
        .chantal("chantal")
        .justin("justin");
    assertThat(bob, is(Bob.create("kevin", "chantal", "justin")));
    assertThat(bob.toBuilder().build(),
        is(Bob.create("kevin", "chantal", "justin")));
    assertThat(bob.toBuilder().kevin("bob").build(),
        is(Bob.create("bob", "chantal", "justin")));
    assertThat(bob.toBuilder().chantal("bob").build(),
        is(Bob.create("kevin", "bob", "justin")));
    assertThat(bob.toBuilder().justin("bob").build(),
        is(Bob.create("kevin", "chantal", "bob")));
    assertThat(bob.toBuilder().kevin("bob").chantal("bob").build(),
        is(Bob.create("bob", "bob", "justin")));
    assertThat(bob.toBuilder().kevin("bob").justin("bob").build(),
        is(Bob.create("bob", "chantal", "bob")));
    assertThat(bob.toBuilder().chantal("bob").justin("bob").build(),
        is(Bob.create("kevin", "bob", "bob")));
    assertThat(bob.toBuilder().kevin("bob").chantal("bob").justin("bob").build(),
        is(Bob.create("bob", "bob", "bob")));
  }

  @Test
  public void withers() {
    Bob bob = Bob.create("kevin", "chantal", "justin").withChantal("bob");
    assertThat(bob.withChantal("bob"),
        is(Bob.create("kevin", "bob", "justin")));
    assertThat(bob.withChantal("bob").withKevin("bob"),
        is(Bob.create("bob", "bob", "justin")));
    assertThat(bob.withChantal("bob").withKevin("bob").withJustin("bobby"),
        is(Bob.create("bob", "bob", "bobby")));
  }

  @Test
  public void updateTwice() {
    Bob bob = Bob.create("kevin", "chantal", "justin");
    assertThat(bob.toBuilder().kevin("bob").kevin("bobby").chantal("bob").build(),
        is(Bob.create("bobby", "bob", "justin")));
  }

}