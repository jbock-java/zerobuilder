package net.zerobuilder.examples.inheritance;

abstract class CelestialBody {

  private final long mass;

  protected CelestialBody(long mass) {
    this.mass = mass;
  }

  long getMass() {
    return mass;
  }
}
