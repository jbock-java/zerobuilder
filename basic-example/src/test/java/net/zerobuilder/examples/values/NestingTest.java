package net.zerobuilder.examples.values;

import net.zerobuilder.examples.values.Nesting.CrowsNest;
import net.zerobuilder.examples.values.Nesting.CrowsNest.LizardsNest;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class NestingTest {

  @Test
  void testDovesNest() {
    Nesting.DovesNest dovesNest = Nesting_DovesNestBuilders.builder().smallEgg(5).regularEgg(12);
    assertEquals(12, dovesNest.regularEgg);
    assertEquals(5, dovesNest.smallEgg);
    dovesNest = Nesting_DovesNestBuilders.builder(dovesNest).regularEgg(8).build();
    assertEquals(8, dovesNest.regularEgg);
    assertEquals(5, dovesNest.smallEgg);
  }

  @Test
  void testCrowsNest() {
    CrowsNest crowsNest = Nesting_CrowsNestBuilders.builder().largeEgg(5).hugeEgg(12);
    assertEquals(12, crowsNest.hugeEgg);
    assertEquals(5, crowsNest.largeEgg);
    crowsNest = Nesting_CrowsNestBuilders.builder(crowsNest).hugeEgg(8).build();
    assertEquals(8, crowsNest.hugeEgg);
    assertEquals(5, crowsNest.largeEgg);
  }

  @Test
  void testLizardsNest() {
    LizardsNest crowsNest = Nesting_CrowsNest_LizardsNestBuilders.builder().spottedEgg(1);
    assertEquals(1, crowsNest.spottedEgg);
    crowsNest = Nesting_CrowsNest_LizardsNestBuilders.builder(crowsNest).spottedEgg(2).build();
    assertEquals(2, crowsNest.spottedEgg);
  }
}
