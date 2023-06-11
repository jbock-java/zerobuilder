package net.zerobuilder.examples.generics;

import org.junit.jupiter.api.Test;

import java.io.IOException;

import static net.zerobuilder.examples.generics.GenericConstructorBuilders.genericConstructorBuilder;
import static org.junit.jupiter.api.Assertions.assertEquals;

public class GenericConstructorTest {

  @Test
  public void testConstructor() throws IOException {
    GenericConstructor<String, Integer> entry = genericConstructorBuilder()
        .key("a")
        .value(2);
    assertEquals("a", entry.getKey());
    assertEquals(2, entry.getValue());
  }
}
