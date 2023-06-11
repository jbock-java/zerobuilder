package net.zerobuilder.examples.values;


import net.zerobuilder.Builder;
import net.zerobuilder.Recycle;
import net.zerobuilder.Updater;

// projections: field access
// see FieldAccessTest
final class FieldAccess {

  final double length;
  final double width;
  final double height;

  @Builder
  @Updater
  @Recycle
  FieldAccess(double length, double width, double height) {
    this.length = length;
    this.width = width;
    this.height = height;
  }
}
