package net.zerobuilder.examples.values.inheritance;

import net.zerobuilder.Builders;
import net.zerobuilder.Goal;

import java.math.BigInteger;

// inheritance + direct field access
@Builders
final class Star extends CelestialBody {

  @Goal(toBuilder = true)
  Star(BigInteger mass) {
    super(mass);
  }
}
