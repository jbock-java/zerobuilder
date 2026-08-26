package net.zerobuilder.examples.values.inheritance;

import java.math.BigInteger;
import net.zerobuilder.RecordBuilder;

// inheritance + direct field access
@RecordBuilder
final class Star extends CelestialBody {

  Star(BigInteger mass) {
    super(mass);
  }
}
