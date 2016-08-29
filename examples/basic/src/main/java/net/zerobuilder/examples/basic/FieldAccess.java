package net.zerobuilder.examples.basic;

import net.zerobuilder.Build;

@Build
class FieldAccess {

  final double lenght;
  final double width;
  final double height;

  @Build.Via
  FieldAccess(double lenght, double width, double height) {
    this.lenght = lenght;
    this.width = width;
    this.height = height;
  }

}
