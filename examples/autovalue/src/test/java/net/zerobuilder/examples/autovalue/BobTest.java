package net.zerobuilder.examples.autovalue;

import org.junit.Test;

import static org.hamcrest.core.Is.is;
import static org.junit.Assert.assertThat;

public class BobTest {

  @Test
  public void updateEveryCombination() {
    Bob bob = BobBuilders.bobBuilder()
        .kevin("kevin")
        .chantal("chantal")
        .justin("justin");
    assertThat(bob, is(Bob.create("kevin", "chantal", "justin")));
    assertThat(bob.updater().done(),
        is(Bob.create("kevin", "chantal", "justin")));
    assertThat(bob.updater().kevin("bob").done(),
        is(Bob.create("bob", "chantal", "justin")));
    assertThat(bob.updater().chantal("bob").done(),
        is(Bob.create("kevin", "bob", "justin")));
    assertThat(bob.updater().justin("bob").done(),
        is(Bob.create("kevin", "chantal", "bob")));
    assertThat(bob.updater().kevin("bob").chantal("bob").done(),
        is(Bob.create("bob", "bob", "justin")));
    assertThat(bob.updater().kevin("bob").justin("bob").done(),
        is(Bob.create("bob", "chantal", "bob")));
    assertThat(bob.updater().chantal("bob").justin("bob").done(),
        is(Bob.create("kevin", "bob", "bob")));
    assertThat(bob.updater().kevin("bob").chantal("bob").justin("bob").done(),
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
    assertThat(bob.updater().kevin("bob").kevin("bobby").chantal("bob").done(),
        is(Bob.create("bobby", "bob", "justin")));
  }

}