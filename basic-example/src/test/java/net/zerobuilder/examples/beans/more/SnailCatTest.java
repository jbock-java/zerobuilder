package net.zerobuilder.examples.beans.more;

import java.util.List;
import org.junit.jupiter.api.Test;

import static net.zerobuilder.examples.beans.more.SnailCatBuilders.snailCatBuilder;
import static net.zerobuilder.examples.beans.more.SnailCatBuilders.snailCatUpdater;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class SnailCatTest {

  @Test
  void testSlow() {
    SnailCat<String> peter = snailCatBuilder()
        .name("Peter")
        .age(36)
        .notes(List.of("entry"))
        .executive(false);
    SnailCat<String> updated = snailCatUpdater(peter)
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
