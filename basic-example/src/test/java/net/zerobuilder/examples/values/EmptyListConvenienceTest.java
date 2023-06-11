package net.zerobuilder.examples.values;

import org.junit.jupiter.api.Test;

import static java.util.Collections.emptyList;
import static java.util.Collections.emptySet;
import static java.util.Collections.singletonList;
import static net.zerobuilder.examples.values.EmptyListConvenienceBuilders.emptyListConvenienceBuilder;
import static net.zerobuilder.examples.values.EmptyListConvenienceBuilders.emptyListConvenienceUpdater;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;

public class EmptyListConvenienceTest {

  @Test
  public void emptyTest() {
    EmptyListConvenience empty = emptyListConvenienceBuilder()
        .things(emptyList())
        .strings(emptyList())
        .collection(emptyList())
        .iterables(emptyList())
        .sets(emptySet());
    EmptyListConvenience notEmpty = emptyListConvenienceUpdater(empty)
        .strings(singletonList(""))
        .things(singletonList(""))
        .collection(singletonList(singletonList("")))
        .sets(emptySet())
        .iterables(emptyList())
        .done();
    assertEquals(0, empty.collection.size());
    assertEquals(0, empty.sets.size());
    assertEquals(0, empty.strings.size());
    assertEquals(0, empty.things.size());
    assertFalse(empty.iterables.iterator().hasNext());
    assertEquals(1, notEmpty.strings.size());
    assertEquals(1, notEmpty.things.size());
    assertEquals(1, notEmpty.collection.size());
    assertEquals(0, notEmpty.sets.size());
    assertFalse(notEmpty.iterables.iterator().hasNext());
  }
}
