package net.zerobuilder.examples.basic;

import net.zerobuilder.Builders;
import net.zerobuilder.Goal;

@Builders
final class GoalGetters {

  @Goal
  static Getters create(double lenght, double width, double height) {
    return new Getters(lenght, width, height);
  }

}
