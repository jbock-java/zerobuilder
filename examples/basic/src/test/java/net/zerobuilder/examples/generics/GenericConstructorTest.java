package net.zerobuilder.examples.generics;

import org.junit.Test;

import java.io.IOException;

import static net.zerobuilder.examples.generics.GenericConstructorBuilders.genericConstructorBuilder;
import static org.hamcrest.core.Is.is;
import static org.junit.Assert.assertThat;

public class GenericConstructorTest {

  @Test
  public void testConstructor() throws IOException {
    GenericConstructor<String, Integer> entry = genericConstructorBuilder()
        .key("a")
        .value(2);
    assertThat(entry.getKey(), is("a"));
    assertThat(entry.getValue(), is(2));
  }
}
