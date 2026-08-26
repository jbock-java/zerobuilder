package net.zerobuilder.examples.values;

import net.zerobuilder.RecordBuilder;

// projections: getters
// see GettersTest
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

  double getLength() {
    return length;
  }

  double getWidth() {
    return width;
  }

  double getHeight() {
    return height;
  }
}
