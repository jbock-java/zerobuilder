package net.zerobuilder.examples.basic;

import net.zerobuilder.Build;
import net.zerobuilder.Goal;

import java.io.IOException;

@Build
final class Throw {

  @Goal
  static void doUpdate(String message) throws IOException {
    throw new IOException(message);
  }
}
