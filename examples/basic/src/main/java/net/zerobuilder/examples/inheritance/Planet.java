package net.zerobuilder.examples.inheritance;

import net.zerobuilder.Build;

import java.math.BigInteger;

@Build
final class Planet extends CelestialBody {
  final int numberOfMoons;
  private final boolean habitable;

  @Build.Goal(toBuilder = true)
  Planet(BigInteger mass, int numberOfMoons, boolean habitable) {
    super(mass);
    this.numberOfMoons = numberOfMoons;
    this.habitable = habitable;
  }

  boolean isHabitable() {
    return habitable;
  }
}
