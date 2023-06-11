package net.zerobuilder.examples.values.inheritance;

import net.zerobuilder.Builder;
import net.zerobuilder.Updater;

import java.math.BigInteger;

// inheritance + overrides
final class Planet extends CelestialBody implements IMoons {

  private final int numberOfMoons;
  private final boolean habitable;

  @Builder
  @Updater
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
