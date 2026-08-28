package net.zerobuilder.examples.values.inheritance;

import java.math.BigInteger;
import net.zerobuilder.RecordBuilder;

@RecordBuilder
final class Planet {

  final BigInteger mass;
  private final int numberOfMoons;
  private final boolean habitable;

  Planet(BigInteger mass, int numberOfMoons, boolean habitable) {
    this.mass = mass;
    this.numberOfMoons = numberOfMoons;
    this.habitable = habitable;
  }

  BigInteger getMass() {
    return mass;
  }

  public int numberOfMoons() {
    return numberOfMoons;
  }

  boolean habitable() {
    return habitable;
  }
}
