package net.zerobuilder.examples.basic;

import net.zerobuilder.Builders;
import net.zerobuilder.Goal;

import java.io.IOException;

// goal with declared exceptions
@Builders
final class Throw {

  @Goal
  static void doUpdate(String message) throws IOException {
    throw new IOException(message);
  }
}
