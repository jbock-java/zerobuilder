package net.zerobuilder.examples.values.inheritance;

import net.zerobuilder.Builder;
import net.zerobuilder.Updater;

import java.math.BigInteger;

// inheritance + direct field access
final class Star extends CelestialBody {

  @Builder
  @Updater
  Star(BigInteger mass) {
    super(mass);
  }
}
