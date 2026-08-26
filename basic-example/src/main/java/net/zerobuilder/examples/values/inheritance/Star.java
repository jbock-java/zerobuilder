package net.zerobuilder.examples.values.inheritance;

import java.math.BigInteger;
import net.zerobuilder.Builder;

// inheritance + direct field access
final class Star extends CelestialBody {

  @Builder
  Star(BigInteger mass) {
    super(mass);
  }
}
