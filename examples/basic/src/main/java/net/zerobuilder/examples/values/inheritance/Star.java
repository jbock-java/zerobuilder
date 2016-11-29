package net.zerobuilder.examples.values.inheritance;

import net.zerobuilder.Goal;

import java.math.BigInteger;

// inheritance + direct field access
final class Star extends CelestialBody {

  @Goal(updater = true)
  Star(BigInteger mass) {
    super(mass);
  }
}
