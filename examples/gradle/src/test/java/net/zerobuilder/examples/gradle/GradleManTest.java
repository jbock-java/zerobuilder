package net.zerobuilder.examples.gradle;

import org.junit.Test;

import static org.hamcrest.core.Is.is;
import static org.junit.Assert.assertThat;

public class GradleManTest {

  @Test
  public void test() {
    GradleMan gradleMan = GradleManBuilders.gradleManBuilder().message("Hello gradle!");
    assertThat(gradleMan.message, is("Hello gradle!"));
  }
}
