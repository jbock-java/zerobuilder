package net.zerobuilder.examples.values;

import net.zerobuilder.RecordBuilder;

@RecordBuilder
final class Getters {

  private final double length;
  private final double width;
  private final double height;

  Getters(double length, double width, double height) {
    this.length = length;
    this.width = width;
    this.height = height;
  }

  double length() {
    return length;
  }

  double width() {
    return width;
  }

  double height() {
    return height;
  }
}
