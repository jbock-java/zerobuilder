package net.zerobuilder.examples.gradle;

import net.zerobuilder.RecordBuilder;

@RecordBuilder
public final class GradleMan {

  final String message;

  GradleMan (String message) {
    this.message = message;
  }
}
