package net.zerobuilder.examples.beans;

import org.junit.Test;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;

import static java.util.Collections.singletonList;
import static java.util.Collections.unmodifiableCollection;
import static net.zerobuilder.examples.beans.EmptyBeanConvenienceBuilders.emptyBeanConvenienceBuilder;
import static net.zerobuilder.examples.beans.EmptyBeanConvenienceBuilders.emptyBeanConvenienceUpdater;
import static org.hamcrest.core.Is.is;
import static org.junit.Assert.assertThat;

public class EmptyBeanConvenienceTest {

  @Test
  public void getThings() throws Exception {
    EmptyBeanConvenience empty = emptyBeanConvenienceBuilder()
        .emptyCollection()
        .emptyIterables()
        .emptySets()
        .emptyStrings()
        .emptyThings();
    EmptyBeanConvenience notEmpty = emptyBeanConvenienceUpdater(empty)
        .strings(singletonList(""))
        .things(singletonList(""))
        .collection(singletonList(singletonList("")))
        .emptyIterables()
        .emptySets()
        .done();
    assertThat(empty.getStrings().size(), is(0));
    assertThat(empty.getThings().size(), is(0));
    assertThat(empty.getCollection().size(), is(0));
    assertThat(empty.getSets().size(), is(0));
    assertThat(empty.getIterables().iterator().hasNext(), is(false));
    assertThat(notEmpty.getStrings().size(), is(1));
    assertThat(notEmpty.getThings().size(), is(1));
    assertThat(notEmpty.getCollection().size(), is(1));
    assertThat(notEmpty.getSets().size(), is(0));
    assertThat(notEmpty.getIterables().iterator().hasNext(), is(false));
  }

}