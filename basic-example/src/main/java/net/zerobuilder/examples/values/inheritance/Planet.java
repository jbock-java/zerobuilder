package net.zerobuilder.examples.values.inheritance;

import java.math.BigInteger;
import net.zerobuilder.Builder;

// inheritance + overrides
final class Planet extends CelestialBody implements IMoons {

  private final int numberOfMoons;
  private final boolean habitable;

  @Builder
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
