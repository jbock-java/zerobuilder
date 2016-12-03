package net.zerobuilder.examples.values;


import net.zerobuilder.Builder;
import net.zerobuilder.Updater;

// nested classes
final class Nesting {

  static class DovesNest {
    final int smallEgg;
    final int regularEgg;

    @Builder
    @Updater
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

      @Builder
      @Updater
      LizardsNest(int spottedEgg) {
        this.spottedEgg = spottedEgg;
      }
    }

    @Builder
    @Updater
    CrowsNest(int largeEgg, int hugeEgg) {
      this.largeEgg = largeEgg;
      this.hugeEgg = hugeEgg;
    }
  }

}
