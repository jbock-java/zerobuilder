package net.zerobuilder.examples.basic;

import net.zerobuilder.Build;
import net.zerobuilder.Goal;

@Build
final class GoalGetters {

  @Goal
  static Getters create(double lenght, double width, double height) {
    return new Getters(lenght, width, height);
  }

}
