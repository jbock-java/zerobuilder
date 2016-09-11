package net.zerobuilder.examples.basic;

import net.zerobuilder.Builders;
import net.zerobuilder.Goal;

@Builders(recycle = true)
final class FieldAccess {

  final double length;
  final double width;
  final double height;

  @Goal(toBuilder = true)
  FieldAccess(double length, double width, double height) {
    this.length = length;
    this.width = width;
    this.height = height;
  }

}
