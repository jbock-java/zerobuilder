package net.zerobuilder.examples.values.inheritance;

import java.math.BigInteger;

abstract class CelestialBody {

  final BigInteger mass;

  protected CelestialBody(BigInteger mass) {
    this.mass = mass;
  }

  BigInteger getMass() {
    return BigInteger.ZERO;
  }
}
