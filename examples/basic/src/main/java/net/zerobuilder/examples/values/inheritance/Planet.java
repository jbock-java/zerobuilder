package net.zerobuilder.examples.values.inheritance;

import net.zerobuilder.Builders;
import net.zerobuilder.Goal;

import java.math.BigInteger;

// inheritance + overrides
@Builders
final class Planet extends CelestialBody implements IMoons {
  private final int numberOfMoons;
  private final boolean habitable;

  @Goal(updater = true)
  Planet(BigInteger mass, int numberOfMoons, boolean habitable) {
    super(mass);
    this.numberOfMoons = numberOfMoons;
    this.habitable = habitable;
  }

  @Override
  BigInteger getMass() {
    return super.mass;
  }

  @Override
  public int getNumberOfMoons() {
    return numberOfMoons;
  }

  boolean isHabitable() {
    return habitable;
  }
}
