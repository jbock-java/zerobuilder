package net.zerobuilder.examples.basic;

import net.zerobuilder.Build;
import net.zerobuilder.Goal;

final class Nesting {

  @Build
  static class DovesNest {
    final int smallEgg;
    final int regularEgg;

    @Goal(toBuilder = true)
    DovesNest(int smallEgg, int regularEgg) {
      this.smallEgg = smallEgg;
      this.regularEgg = regularEgg;
    }
  }

  @Build
  static class CrowsNest {
    final int largeEgg;
    final int hugeEgg;

    @Build
    static class LizardsNest {
      final int spottedEgg;

      @Goal(toBuilder = true)
      LizardsNest(int spottedEgg) {
        this.spottedEgg = spottedEgg;
      }
    }

    @Goal(toBuilder = true)
    CrowsNest(int largeEgg, int hugeEgg) {
      this.largeEgg = largeEgg;
      this.hugeEgg = hugeEgg;
    }
  }

}
