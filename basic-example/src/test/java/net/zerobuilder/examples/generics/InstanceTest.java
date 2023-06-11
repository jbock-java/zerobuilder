package net.zerobuilder.examples.generics;

import org.junit.jupiter.api.Test;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;

public class InstanceTest {

  @Test
  public void entry() {
    Instance<String> instance = new Instance<>("");
    Map.Entry<Integer, String> entry = InstanceBuilders.entryBuilder(instance)
        .key(1)
        .value("y");
    assertEquals(1, entry.getKey());
    assertEquals("y", entry.getValue());
  }

  @Test
  public void ventry() {
    Instance<String> instance = new Instance<>("");
    Map.Entry<String, String> entry = InstanceBuilders.ventryBuilder(instance)
        .value("1");
    assertEquals("1", entry.getKey());
    assertEquals("1", entry.getValue());
  }
}
