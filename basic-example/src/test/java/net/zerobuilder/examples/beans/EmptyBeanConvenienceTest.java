package net.zerobuilder.examples.beans;

import org.junit.jupiter.api.Test;

import static java.util.Collections.emptyList;
import static java.util.Collections.emptySet;
import static java.util.Collections.singletonList;
import static net.zerobuilder.examples.beans.EmptyBeanConvenienceBuilders.emptyBeanConvenienceBuilder;
import static net.zerobuilder.examples.beans.EmptyBeanConvenienceBuilders.emptyBeanConvenienceUpdater;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;

public class EmptyBeanConvenienceTest {

  @Test
  public void getThings() {
    EmptyBeanConvenience empty = emptyBeanConvenienceBuilder()
        .collection(emptyList())
        .iterables(emptyList())
        .sets(emptySet())
        .strings(emptyList())
        .things(emptyList());
    EmptyBeanConvenience notEmpty = emptyBeanConvenienceUpdater(empty)
        .strings(singletonList(""))
        .things(singletonList(""))
        .collection(singletonList(singletonList("")))
        .iterables(emptyList())
        .sets(emptySet())
        .done();
    assertEquals(0, empty.getStrings().size());
    assertEquals(0, empty.getThings().size());
    assertEquals(0, empty.getCollection().size());
    assertEquals(0, empty.getSets().size());
    assertFalse(empty.getIterables().iterator().hasNext());
    assertEquals(1, notEmpty.getStrings().size());
    assertEquals(1, notEmpty.getThings().size());
    assertEquals(1, notEmpty.getCollection().size());
    assertEquals(0, notEmpty.getSets().size());
    assertFalse(notEmpty.getIterables().iterator().hasNext());
  }

}
