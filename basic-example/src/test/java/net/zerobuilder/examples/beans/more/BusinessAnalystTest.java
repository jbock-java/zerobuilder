package net.zerobuilder.examples.beans.more;

import org.junit.jupiter.api.Test;

import static java.util.Arrays.asList;
import static java.util.Collections.singletonList;
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
        .notes(asList("entry"));
    BusinessAnalyst updated = businessAnalystUpdater(peter)
        .executive(true)
        .age(37)
        .notes(asList("entry0", "entry1"))
        .done();
    assertEquals(peter.getAge(), 36);
    assertEquals(peter.getName(), "Peter");
    assertEquals(peter.getNotes(), singletonList("entry"));
    assertFalse(peter.isExecutive());
    assertEquals(updated.getAge(), 37);
    assertEquals(updated.getName(), "Peter");
    assertEquals(updated.getNotes(), asList("entry0", "entry1"));
    assertTrue(updated.isExecutive());
  }

  @Test
  public void testCollectionTwoToOne() {
    BusinessAnalyst peter = businessAnalystBuilder()
        .age(36)
        .executive(true)
        .name("Peter")
        .notes(asList("entry0", "entry1"));
    BusinessAnalyst updated = businessAnalystUpdater(peter)
        .age(37)
        .executive(false)
        .notes(singletonList("entry"))
        .done();
    assertEquals(peter.getAge(), 36);
    assertEquals(peter.getName(), "Peter");
    assertEquals(peter.getNotes(), asList("entry0", "entry1"));
    assertTrue(peter.isExecutive());
    assertEquals(updated.getAge(), 37);
    assertEquals(updated.getName(), "Peter");
    assertEquals(updated.getNotes(), singletonList("entry"));
    assertFalse(updated.isExecutive());
  }
}
