package net.zerobuilder.examples.basic;

import net.zerobuilder.Build;

import java.io.IOException;

@Build
final class Throw {
  static void doUpdate(String message) throws IOException {
    throw new IOException(message);
  }
}
