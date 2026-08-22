package net.zerobuilder.examples.values;

import net.zerobuilder.examples.values.Nesting.CrowsNest;
import net.zerobuilder.examples.values.Nesting.CrowsNest.LizardsNest;
import org.junit.jupiter.api.Test;

import static net.zerobuilder.examples.values.Nesting_CrowsNestBuilders.crowsNestBuilder;
import static net.zerobuilder.examples.values.Nesting_CrowsNestBuilders.crowsNestUpdater;
import static net.zerobuilder.examples.values.Nesting_CrowsNest_LizardsNestBuilders.lizardsNestBuilder;
import static net.zerobuilder.examples.values.Nesting_CrowsNest_LizardsNestBuilders.lizardsNestUpdater;
import static net.zerobuilder.examples.values.Nesting_DovesNestBuilders.dovesNestBuilder;
import static net.zerobuilder.examples.values.Nesting_DovesNestBuilders.dovesNestUpdater;
import static org.junit.jupiter.api.Assertions.assertEquals;

class NestingTest {

  @Test
  void testDovesNest() {
    Nesting.DovesNest dovesNest = dovesNestBuilder().smallEgg(5).regularEgg(12);
    assertEquals(12, dovesNest.regularEgg);
    assertEquals(5, dovesNest.smallEgg);
    dovesNest = dovesNestUpdater(dovesNest).regularEgg(8).build();
    assertEquals(8, dovesNest.regularEgg);
    assertEquals(5, dovesNest.smallEgg);
  }

  @Test
  void testCrowsNest() {
    CrowsNest crowsNest = crowsNestBuilder().largeEgg(5).hugeEgg(12);
    assertEquals(12, crowsNest.hugeEgg);
    assertEquals(5, crowsNest.largeEgg);
    crowsNest = crowsNestUpdater(crowsNest).hugeEgg(8).build();
    assertEquals(8, crowsNest.hugeEgg);
    assertEquals(5, crowsNest.largeEgg);
  }

  @Test
  void testLizardsNest() {
    LizardsNest crowsNest = lizardsNestBuilder().spottedEgg(1);
    assertEquals(1, crowsNest.spottedEgg);
    crowsNest = lizardsNestUpdater(crowsNest).spottedEgg(2).build();
    assertEquals(2, crowsNest.spottedEgg);
  }

}
