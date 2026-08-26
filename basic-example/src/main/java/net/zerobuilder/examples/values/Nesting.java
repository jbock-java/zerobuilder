package net.zerobuilder.examples.values;

import net.zerobuilder.RecordBuilder;

// nested classes
final class Nesting {

  @RecordBuilder
  static class DovesNest {
    final int smallEgg;
    final int regularEgg;

    DovesNest(int smallEgg, int regularEgg) {
      this.smallEgg = smallEgg;
      this.regularEgg = regularEgg;
    }
  }

  @RecordBuilder
  static class CrowsNest {
    final int largeEgg;
    final int hugeEgg;

    @RecordBuilder
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
