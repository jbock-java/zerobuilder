package net.zerobuilder.examples.inheritance;

import net.zerobuilder.Build;

@Build(toBuilder = true)
final class Planet extends CelestialBody {
  final int numberOfMoons;
  private final boolean habitable;

  Planet(long mass, int numberOfMoons, boolean habitable) {
    super(mass);
    this.numberOfMoons = numberOfMoons;
    this.habitable = habitable;
  }

  boolean isHabitable() {
    return habitable;
  }
}
