package net.zerobuilder.examples.values;

import org.junit.Test;

import static java.util.Collections.emptyList;
import static java.util.Collections.emptySet;
import static java.util.Collections.singletonList;
import static net.zerobuilder.examples.values.EmptyListConvenienceBuilders.emptyListConvenienceBuilder;
import static net.zerobuilder.examples.values.EmptyListConvenienceBuilders.emptyListConvenienceUpdater;
import static org.hamcrest.core.Is.is;
import static org.junit.Assert.assertThat;

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
    assertThat(empty.collection.size(), is(0));
    assertThat(empty.sets.size(), is(0));
    assertThat(empty.strings.size(), is(0));
    assertThat(empty.things.size(), is(0));
    assertThat(empty.iterables.iterator().hasNext(), is(false));
    assertThat(notEmpty.strings.size(), is(1));
    assertThat(notEmpty.things.size(), is(1));
    assertThat(notEmpty.collection.size(), is(1));
    assertThat(notEmpty.sets.size(), is(0));
    assertThat(notEmpty.iterables.iterator().hasNext(), is(false));
  }
}