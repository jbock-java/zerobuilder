package net.zerobuilder.examples.basic;

import net.zerobuilder.Builder;
import net.zerobuilder.Goal;

import java.io.IOException;

@Builder
final class Throw {

  @Goal
  static void doUpdate(String message) throws IOException {
    throw new IOException(message);
  }
}
