package net.zerobuilder.examples.values;

import java.util.List;
import java.util.Set;
import org.junit.jupiter.api.Test;

import static net.zerobuilder.examples.values.EmptyListConvenienceBuilders.emptyListConvenienceBuilder;
import static net.zerobuilder.examples.values.EmptyListConvenienceBuilders.emptyListConvenienceUpdater;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;

class EmptyListConvenienceTest {

  @Test
  void emptyTest() {
    EmptyListConvenience empty = emptyListConvenienceBuilder()
        .things(List.of())
        .strings(List.of())
        .collection(List.of())
        .iterables(List.of())
        .sets(Set.of());
    EmptyListConvenience notEmpty = emptyListConvenienceUpdater(empty)
        .strings(List.of(""))
        .things(List.of(""))
        .collection(List.of(List.of("")))
        .sets(Set.of())
        .iterables(List.of())
        .build();
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
