package net.zerobuilder.examples.gradle;

import net.zerobuilder.Builder;

public final class GradleMan {

  final String message;

  @Builder
  GradleMan (String message) {
    this.message = message;
  }
}
