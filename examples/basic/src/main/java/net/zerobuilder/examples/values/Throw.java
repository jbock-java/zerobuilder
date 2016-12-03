package net.zerobuilder.examples.values;

import net.zerobuilder.Builder;

import java.io.IOException;

// goal with declared exceptions
final class Throw<S> {

  final S string;

  Throw(S string) {
    this.string = string;
  }

  @Builder
  static void doUpdate(String message) throws IOException {
    throw new IOException(message);
  }
}
