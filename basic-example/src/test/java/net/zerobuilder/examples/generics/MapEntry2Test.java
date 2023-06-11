package net.zerobuilder.examples.generics;

import org.junit.jupiter.api.Test;

import java.util.Map.Entry;

import static net.zerobuilder.examples.generics.MapEntry2Builders.entryBuilder;
import static org.junit.jupiter.api.Assertions.assertEquals;

public class MapEntry2Test {

  @Test
  public void sentry() {
    Entry<String, String> entry = entryBuilder()
        .key("foo")
        .value("bar");
    assertEquals("foo", entry.getKey());
    assertEquals("bar", entry.getValue());
  }
}
