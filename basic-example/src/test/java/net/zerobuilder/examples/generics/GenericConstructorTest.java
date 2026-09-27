package net.zerobuilder.examples.generics;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

public class GenericConstructorTest {

  @Test
  public void testConstructor() {
    GenericConstructor<String, Integer> entry = GenericConstructorBuilders.<String, Integer>builder()
        .key("a")
        .value(2)
        .build();
    assertEquals("a", entry.getKey());
    assertEquals(2, entry.getValue());
  }
}
