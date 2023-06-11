package net.zerobuilder.examples.generics;

import org.junit.jupiter.api.Test;

import java.util.Map.Entry;

import static net.zerobuilder.examples.generics.MapEntryBuilders.entryBuilder;
import static org.junit.jupiter.api.Assertions.assertEquals;

public class MapEntryTest {

  @Test
  public void entry() {
    Entry<String, Long> entry = entryBuilder()
        .key("foo")
        .value(12L);
    assertEquals("foo", entry.getKey());
    assertEquals(12L, entry.getValue());
  }
}
