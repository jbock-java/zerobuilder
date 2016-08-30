package net.zerobuilder.examples.basic;

import org.junit.Test;

import static org.hamcrest.core.Is.is;
import static org.junit.Assert.assertThat;

public class NestingTest {

  @Test
  public void testDovesNest() {
    Nesting.DovesNest dovesNest = Nesting_DovesNestBuilder.builder().smallEgg(5).regularEgg(12);
    assertThat(dovesNest.regularEgg, is(12));
    assertThat(dovesNest.smallEgg, is(5));
    dovesNest = Nesting_DovesNestBuilder.toBuilder(dovesNest).regularEgg(8).build();
    assertThat(dovesNest.regularEgg, is(8));
    assertThat(dovesNest.smallEgg, is(5));
  }

  @Test
  public void testCrowsNest() {
    Nesting.CrowsNest crowsNest = Nesting_CrowsNestBuilder.builder().largeEgg(5).hugeEgg(12);
    assertThat(crowsNest.hugeEgg, is(12));
    assertThat(crowsNest.largeEgg, is(5));
    crowsNest = Nesting_CrowsNestBuilder.toBuilder(crowsNest).hugeEgg(8).build();
    assertThat(crowsNest.hugeEgg, is(8));
    assertThat(crowsNest.largeEgg, is(5));
  }

}