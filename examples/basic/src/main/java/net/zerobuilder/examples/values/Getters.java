package net.zerobuilder.examples.values;

import net.zerobuilder.Builder;
import net.zerobuilder.Updater;

// projections: getters
// see GettersTest
final class Getters {

  private final double lenght;
  private final double width;
  private final double height;

  @Builder
  @Updater
  Getters(double lenght, double width, double height) {
    this.lenght = lenght;
    this.width = width;
    this.height = height;
  }

  double getLenght() {
    return lenght;
  }

  double getWidth() {
    return width;
  }

  double getHeight() {
    return height;
  }
}
