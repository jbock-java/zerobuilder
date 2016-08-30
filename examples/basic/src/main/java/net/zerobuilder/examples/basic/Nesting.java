package net.zerobuilder.examples.basic;

import net.zerobuilder.Build;

class Nesting {

  @Build
  static class DovesNest {

    final int smallEgg;
    final int regularEgg;

    @Build.Via
    DovesNest(int smallEgg, int regularEgg) {
      this.smallEgg = smallEgg;
      this.regularEgg = regularEgg;
    }

  }

  @Build
  static class CrowsNest {
    final int largeEgg;
    final int hugeEgg;

    @Build.Via
    CrowsNest(int largeEgg, int hugeEgg) {
      this.largeEgg = largeEgg;
      this.hugeEgg = hugeEgg;
    }

  }

}
