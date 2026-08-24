package net.zerobuilder.examples.gradle;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

public class GradleManTest {

  @Test
  public void test() {
    GradleMan gradleMan = GradleManBuilders.builder().message("Hello gradle!");
    assertEquals("Hello gradle!", gradleMan.message);
  }
}
