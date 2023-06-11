package net.zerobuilder.examples.generics;

import org.junit.jupiter.api.Test;

import java.util.Map.Entry;

import static java.util.Arrays.asList;
import static net.zerobuilder.examples.generics.Test4Builders.entryBuilder;
import static org.junit.jupiter.api.Assertions.assertEquals;

public class Test4Test {

  @Test
  public void entry() throws Exception {
    Entry<String, Integer> entry = entryBuilder()
        .key("a")
        .value(1)
        .ks(asList("b", "c"))
        .value2(25);
    assertEquals("abc", entry.getKey());
    assertEquals(26, entry.getValue());
  }
}
