package net.zerobuilder.examples.beans;

import org.junit.Test;

import static org.hamcrest.core.Is.is;
import static org.junit.Assert.assertThat;

public class EmptyBeanConvenienceTest {
  
  @Test
  public void getThings() throws Exception {
    EmptyBeanConvenience empty = EmptyBeanConvenienceBuilders.emptyBeanConvenienceBuilder()
        .emptyCollection()
        .emptyIterables()
        .emptySets()
        .emptyStrings()
        .emptyThings();
    assertThat(empty.getCollection().size(), is(0));
    assertThat(empty.getSets().size(), is(0));
    assertThat(empty.getStrings().size(), is(0));
    assertThat(empty.getThings().size(), is(0));
    assertThat(empty.getIterables().iterator().hasNext(), is(false));

  }

}