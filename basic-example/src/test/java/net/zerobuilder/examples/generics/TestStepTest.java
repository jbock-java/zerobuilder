package net.zerobuilder.examples.generics;

import org.junit.jupiter.api.Test;

import java.util.Map.Entry;

import static net.zerobuilder.examples.generics.TestStepBuilders.entryBuilder;
import static org.junit.jupiter.api.Assertions.assertEquals;

public class TestStepTest {

  @Test
  public void entry() {
    Entry<String, Integer> entry = entryBuilder()
        .value(12)
        .key("a")
        .suffix(12);
    assertEquals("a12", entry.getKey());
    assertEquals(12, entry.getValue());
  }
}
