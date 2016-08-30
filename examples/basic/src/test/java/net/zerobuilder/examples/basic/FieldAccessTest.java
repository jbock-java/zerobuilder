package net.zerobuilder.examples.basic;

import org.junit.Test;

import static net.zerobuilder.examples.basic.FieldAccessBuilder.builder;
import static net.zerobuilder.examples.basic.FieldAccessBuilder.toBuilder;
import static org.hamcrest.core.Is.is;
import static org.junit.Assert.*;

public class FieldAccessTest {

  @Test
  public void basicTest() {
    FieldAccess getters = builder().length(12).width(10).height(11);
    assertThat(getters.length, is(12d));
    assertThat(getters.width, is(10d));
    assertThat(getters.height, is(11d));
    getters = toBuilder(getters).length(0).build();
    assertThat(getters.length, is(0d));
    assertThat(getters.width, is(10d));
    assertThat(getters.height, is(11d));
  }


}