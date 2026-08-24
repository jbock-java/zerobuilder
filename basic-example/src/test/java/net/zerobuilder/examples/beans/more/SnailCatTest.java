package net.zerobuilder.examples.beans.more;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class SnailCatTest {

  @Test
  void testSlow() {
    SnailCat<String> peter = SnailCatBuilders.<String>builder()
        .name("Peter")
        .age(36)
        .notes(List.of("entry"))
        .executive(false);
    SnailCat<String> updated = SnailCatBuilders.builder(peter)
        .executive(true)
        .age(37)
        .notes(List.of("entry0", "entry1"))
        .build();
    assertEquals(36, peter.age());
    assertEquals("Peter", peter.name());
    assertEquals(List.of("entry"), peter.notes());
    assertFalse(peter.isExecutive());
    assertEquals(37, updated.age());
    assertEquals("Peter", updated.name());
    assertEquals(List.of("entry0", "entry1"), updated.notes());
    assertTrue(updated.isExecutive());
  }
}
