package net.zerobuilder.examples.values;

import org.junit.Test;

import static org.hamcrest.core.Is.is;
import static org.junit.Assert.assertThat;

public class GettersTest {

  @Test
  public void basicTest() {
    Getters getters = GettersBuilders.gettersBuilder().lenght(12).width(10).height(11);
    assertThat(getters.getLenght(), is(12d));
    assertThat(getters.getWidth(), is(10d));
    assertThat(getters.getHeight(), is(11d));
    getters = GettersBuilders.gettersToBuilder(getters).lenght(0).build();
    assertThat(getters.getLenght(), is(0d));
    assertThat(getters.getWidth(), is(10d));
    assertThat(getters.getHeight(), is(11d));
  }

}