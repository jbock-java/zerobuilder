package net.zerobuilder.examples.values.inheritance;

import net.zerobuilder.RecordBuilder;

import java.math.BigInteger;

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

  public int getNumberOfMoons() {
    return numberOfMoons;
  }

  boolean isHabitable() {
    return habitable;
  }
}
