package net.zerobuilder.examples.basic;

import net.zerobuilder.Build;

@Build(toBuilder = true, nogc = true)
final class FieldAccess {

  final double length;
  final double width;
  final double height;

  FieldAccess(double length, double width, double height) {
    this.length = length;
    this.width = width;
    this.height = height;
  }

}
