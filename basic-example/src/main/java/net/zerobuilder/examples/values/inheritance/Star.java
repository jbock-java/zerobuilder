package net.zerobuilder.examples.values.inheritance;

import net.zerobuilder.RecordBuilder;

import java.math.BigInteger;

@RecordBuilder
final class Star {

  final BigInteger mass;

  Star(BigInteger mass) {
    this.mass = mass;
  }
}
