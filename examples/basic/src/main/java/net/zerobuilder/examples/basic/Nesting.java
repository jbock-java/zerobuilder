package net.zerobuilder.examples.basic;

import net.zerobuilder.Build;

final class Nesting {

  @Build
  static class DovesNest {
    final int smallEgg;
    final int regularEgg;

    @Build.Goal(toBuilder = true)
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

      @Build.Goal(toBuilder = true)
      LizardsNest(int spottedEgg) {
        this.spottedEgg = spottedEgg;
      }
    }

    @Build.Goal(toBuilder = true)
    CrowsNest(int largeEgg, int hugeEgg) {
      this.largeEgg = largeEgg;
      this.hugeEgg = hugeEgg;
    }
  }

}
