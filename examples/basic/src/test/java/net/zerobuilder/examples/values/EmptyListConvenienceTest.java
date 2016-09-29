package net.zerobuilder.examples.values;

import org.junit.Test;

import static net.zerobuilder.examples.values.EmptyListConvenienceBuilders.emptyListConvenienceBuilder;
import static org.hamcrest.core.Is.is;
import static org.junit.Assert.assertThat;

public class EmptyListConvenienceTest {

  @Test
  public void emptyTest() {
    EmptyListConvenience empty = emptyListConvenienceBuilder()
        .emptyThings()
        .emptyStrings()
        .emptyCollection()
        .emptyIterables()
        .emptySets();
    assertThat(empty.collection.size(), is(0));
    assertThat(empty.sets.size(), is(0));
    assertThat(empty.strings.size(), is(0));
    assertThat(empty.things.size(), is(0));
    assertThat(empty.iterables.iterator().hasNext(), is(false));
  }

}