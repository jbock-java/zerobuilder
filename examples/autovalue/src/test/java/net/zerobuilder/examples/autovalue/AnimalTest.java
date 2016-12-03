package net.zerobuilder.examples.autovalue;

import org.junit.Test;

import static org.hamcrest.core.Is.is;
import static org.junit.Assert.assertThat;

public class AnimalTest {

  @Test
  public void builder() throws Exception {
    Animal zebra = Animal.builder().name("foo").numberOfLegs(10);
    Animal updated = zebra.updater().name("clara").done();
    assertThat(zebra.name(), is("foo"));
    assertThat(zebra.numberOfLegs(), is(10));
    assertThat(updated.name(), is("clara"));
    assertThat(updated.numberOfLegs(), is(10));
  }
}