package net.zerobuilder.examples.basic;

import org.junit.Test;

import static net.zerobuilder.examples.basic.Nesting_CrowsNestBuilders.crowsNestBuilder;
import static net.zerobuilder.examples.basic.Nesting_CrowsNest_LizardsNestBuilders.lizardsNestBuilder;
import static net.zerobuilder.examples.basic.Nesting_DovesNestBuilders.dovesNestBuilder;
import static org.hamcrest.core.Is.is;
import static org.junit.Assert.assertThat;

public class NestingTest {

  @Test
  public void testDovesNest() {
    Nesting.DovesNest dovesNest = dovesNestBuilder().smallEgg(5).regularEgg(12);
    assertThat(dovesNest.regularEgg, is(12));
    assertThat(dovesNest.smallEgg, is(5));
    dovesNest = Nesting_DovesNestBuilders.toBuilder(dovesNest).regularEgg(8).build();
    assertThat(dovesNest.regularEgg, is(8));
    assertThat(dovesNest.smallEgg, is(5));
  }

  @Test
  public void testCrowsNest() {
    Nesting.CrowsNest crowsNest = crowsNestBuilder().largeEgg(5).hugeEgg(12);
    assertThat(crowsNest.hugeEgg, is(12));
    assertThat(crowsNest.largeEgg, is(5));
    crowsNest = Nesting_CrowsNestBuilders.toBuilder(crowsNest).hugeEgg(8).build();
    assertThat(crowsNest.hugeEgg, is(8));
    assertThat(crowsNest.largeEgg, is(5));
  }

  @Test
  public void testLizardsNest() {
    Nesting.CrowsNest.LizardsNest crowsNest = lizardsNestBuilder().spottedEgg(1);
    assertThat(crowsNest.spottedEgg, is(1));
    crowsNest = Nesting_CrowsNest_LizardsNestBuilders.toBuilder(crowsNest).spottedEgg(2).build();
    assertThat(crowsNest.spottedEgg, is(2));
  }

}