package net.zerobuilder.examples.basic;

import net.zerobuilder.Builder;
import net.zerobuilder.Goal;

final class Nesting {

  @Builder
  static class DovesNest {
    final int smallEgg;
    final int regularEgg;

    @Goal(toBuilder = true)
    DovesNest(int smallEgg, int regularEgg) {
      this.smallEgg = smallEgg;
      this.regularEgg = regularEgg;
    }
  }

  @Builder
  static class CrowsNest {
    final int largeEgg;
    final int hugeEgg;

    @Builder
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
