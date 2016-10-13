package net.zerobuilder.examples.values;

import net.zerobuilder.Builders;
import net.zerobuilder.Goal;

// projections: field access
// see FieldAccessTest
@Builders(recycle = true)
final class FieldAccess {

  final double length;
  final double width;
  final double height;

  @Goal(updater = true)
  FieldAccess(double length, double width, double height) {
    this.length = length;
    this.width = width;
    this.height = height;
  }

}
