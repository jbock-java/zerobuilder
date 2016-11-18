package net.zerobuilder.examples.generics;

import org.junit.Test;

import static net.zerobuilder.examples.generics.GenericConstructorBuilders.genericConstructorBuilder;
import static net.zerobuilder.examples.generics.GenericConstructorBuilders.genericConstructorUpdater;
import static org.hamcrest.core.Is.is;
import static org.junit.Assert.assertThat;

public class GenericConstructorTest {

  @Test
  public void testConstructor() {
    GenericConstructor<String, Integer> entry = genericConstructorBuilder()
        .key("a")
        .value(2);
    GenericConstructor<String, Integer> modEntry = genericConstructorUpdater(entry)
        .key("foo")
        .value(3)
        .done();
    assertThat(entry.getKey(), is("a"));
    assertThat(entry.getValue(), is(2));
    assertThat(modEntry.getKey(), is("foo"));
    assertThat(modEntry.getValue(), is(3));
  }
}