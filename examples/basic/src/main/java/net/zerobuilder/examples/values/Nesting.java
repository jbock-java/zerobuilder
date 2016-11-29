package net.zerobuilder.examples.values;

import net.zerobuilder.Goal;

// nested classes
final class Nesting {

  static class DovesNest {
    final int smallEgg;
    final int regularEgg;

    @Goal(updater = true)
    DovesNest(int smallEgg, int regularEgg) {
      this.smallEgg = smallEgg;
      this.regularEgg = regularEgg;
    }
  }

  static class CrowsNest {
    final int largeEgg;
    final int hugeEgg;

    static class LizardsNest {
      final int spottedEgg;

      @Goal(updater = true)
      LizardsNest(int spottedEgg) {
        this.spottedEgg = spottedEgg;
      }
    }

    @Goal(updater = true)
    CrowsNest(int largeEgg, int hugeEgg) {
      this.largeEgg = largeEgg;
      this.hugeEgg = hugeEgg;
    }
  }

}
