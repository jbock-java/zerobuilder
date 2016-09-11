package net.zerobuilder.examples.inheritance;

import net.zerobuilder.Builders;
import net.zerobuilder.Goal;

import java.math.BigInteger;

@Builders
final class Planet extends CelestialBody {
  final int numberOfMoons;
  private final boolean habitable;

  @Goal(toBuilder = true)
  Planet(BigInteger mass, int numberOfMoons, boolean habitable) {
    super(mass);
    this.numberOfMoons = numberOfMoons;
    this.habitable = habitable;
  }

  boolean isHabitable() {
    return habitable;
  }
}
