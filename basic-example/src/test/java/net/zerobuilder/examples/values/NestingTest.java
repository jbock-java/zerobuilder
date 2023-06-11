package net.zerobuilder.examples.values;

import org.junit.jupiter.api.Test;

import static net.zerobuilder.examples.values.Nesting_CrowsNestBuilders.crowsNestBuilder;
import static net.zerobuilder.examples.values.Nesting_CrowsNestBuilders.crowsNestUpdater;
import static net.zerobuilder.examples.values.Nesting_CrowsNest_LizardsNestBuilders.lizardsNestBuilder;
import static net.zerobuilder.examples.values.Nesting_CrowsNest_LizardsNestBuilders.lizardsNestUpdater;
import static net.zerobuilder.examples.values.Nesting_DovesNestBuilders.dovesNestBuilder;
import static net.zerobuilder.examples.values.Nesting_DovesNestBuilders.dovesNestUpdater;
import static org.junit.jupiter.api.Assertions.assertEquals;

public class NestingTest {

  @Test
  public void testDovesNest() {
    Nesting.DovesNest dovesNest = dovesNestBuilder().smallEgg(5).regularEgg(12);
    assertEquals(12, dovesNest.regularEgg);
    assertEquals(5, dovesNest.smallEgg);
    dovesNest = dovesNestUpdater(dovesNest).regularEgg(8).done();
    assertEquals(8, dovesNest.regularEgg);
    assertEquals(5, dovesNest.smallEgg);
  }

  @Test
  public void testCrowsNest() {
    Nesting.CrowsNest crowsNest = crowsNestBuilder().largeEgg(5).hugeEgg(12);
    assertEquals(12, crowsNest.hugeEgg);
    assertEquals(5, crowsNest.largeEgg);
    crowsNest = crowsNestUpdater(crowsNest).hugeEgg(8).done();
    assertEquals(8, crowsNest.hugeEgg);
    assertEquals(5, crowsNest.largeEgg);
  }

  @Test
  public void testLizardsNest() {
    Nesting.CrowsNest.LizardsNest crowsNest = lizardsNestBuilder().spottedEgg(1);
    assertEquals(1, crowsNest.spottedEgg);
    crowsNest = lizardsNestUpdater(crowsNest).spottedEgg(2).done();
    assertEquals(2, crowsNest.spottedEgg);
  }

}
