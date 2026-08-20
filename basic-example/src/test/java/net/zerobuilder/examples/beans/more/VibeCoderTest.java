package net.zerobuilder.examples.beans.more;

import java.util.List;
import org.junit.jupiter.api.Test;

import static java.util.Arrays.asList;
import static net.zerobuilder.examples.beans.more.VibeCoderBuilders.vibeCoderBuilder;
import static net.zerobuilder.examples.beans.more.VibeCoderBuilders.vibeCoderUpdater;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class VibeCoderTest {

  @Test
  public void testCollectionOneToTwo() {
    VibeCoder peter = vibeCoderBuilder()
        .name("Peter")
        .age(36)
        .notes(List.of("entry"))
        .isExecutive(false);
    VibeCoder updated = vibeCoderUpdater(peter)
        .isExecutive(true)
        .age(37)
        .notes(asList("entry0", "entry1"))
        .done();
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
