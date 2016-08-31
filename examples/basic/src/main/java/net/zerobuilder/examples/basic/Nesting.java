package net.zerobuilder.examples.basic;

import net.zerobuilder.Build;

class Nesting {

  @Build(toBuilder = true)
  static class DovesNest {
    final int smallEgg;
    final int regularEgg;

    DovesNest(int smallEgg, int regularEgg) {
      this.smallEgg = smallEgg;
      this.regularEgg = regularEgg;
    }
  }

  @Build(toBuilder = true)
  static class CrowsNest {
    final int largeEgg;
    final int hugeEgg;

    @Build(toBuilder = true)
    static class LizardsNest {
      final int spottedEgg;

      LizardsNest(int spottedEgg) {
        this.spottedEgg = spottedEgg;
      }
    }

    CrowsNest(int largeEgg, int hugeEgg) {
      this.largeEgg = largeEgg;
      this.hugeEgg = hugeEgg;
    }
  }

}
