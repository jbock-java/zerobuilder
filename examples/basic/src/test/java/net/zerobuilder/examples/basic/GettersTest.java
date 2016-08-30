package net.zerobuilder.examples.basic;

import org.junit.Test;

import static net.zerobuilder.examples.basic.GettersBuilder.builder;
import static net.zerobuilder.examples.basic.GettersBuilder.toBuilder;
import static org.hamcrest.core.Is.is;
import static org.junit.Assert.assertThat;

public class GettersTest {

  @Test
  public void basicTest() {
    Getters getters = builder().lenght(12).width(10).height(11);
    assertThat(getters.getLenght(), is(12d));
    assertThat(getters.getWidth(), is(10d));
    assertThat(getters.getHeight(), is(11d));
    getters = toBuilder(getters).lenght(0).build();
    assertThat(getters.getLenght(), is(0d));
    assertThat(getters.getWidth(), is(10d));
    assertThat(getters.getHeight(), is(11d));
  }

}