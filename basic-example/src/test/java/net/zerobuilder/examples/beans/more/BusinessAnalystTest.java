package net.zerobuilder.examples.beans.more;

import org.junit.jupiter.api.Test;

import java.util.List;

import static net.zerobuilder.examples.beans.more.BusinessAnalystBuilders.businessAnalystBuilder;
import static net.zerobuilder.examples.beans.more.BusinessAnalystBuilders.businessAnalystUpdater;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class BusinessAnalystTest {

  @Test
  public void testCollectionOneToTwo() {
    BusinessAnalyst peter = businessAnalystBuilder()
        .age(36)
        .executive(false)
        .name("Peter")
        .notes(List.of("entry"));
    BusinessAnalyst updated = businessAnalystUpdater(peter)
        .executive(true)
        .age(37)
        .notes(List.of("entry0", "entry1"))
        .done();
    assertEquals(36, peter.getAge());
    assertEquals("Peter", peter.getName());
    assertEquals(List.of("entry"), peter.getNotes());
    assertFalse(peter.isExecutive());
    assertEquals(37, updated.getAge());
    assertEquals("Peter", updated.getName());
    assertEquals(List.of("entry0", "entry1"), updated.getNotes());
    assertTrue(updated.isExecutive());
  }

  @Test
  public void testCollectionTwoToOne() {
    BusinessAnalyst peter = businessAnalystBuilder()
        .age(36)
        .executive(true)
        .name("Peter")
        .notes(List.of("entry0", "entry1"));
    BusinessAnalyst updated = businessAnalystUpdater(peter)
        .age(37)
        .executive(false)
        .notes(List.of("entry"))
        .done();
    assertEquals(36, peter.getAge());
    assertEquals("Peter", peter.getName());
    assertEquals(List.of("entry0", "entry1"), peter.getNotes());
    assertTrue(peter.isExecutive());
    assertEquals(37, updated.getAge());
    assertEquals("Peter", updated.getName());
    assertEquals(List.of("entry"), updated.getNotes());
    assertFalse(updated.isExecutive());
  }
}
