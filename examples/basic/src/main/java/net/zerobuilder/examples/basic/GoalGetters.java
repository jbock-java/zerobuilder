package net.zerobuilder.examples.basic;

import net.zerobuilder.Builder;
import net.zerobuilder.Goal;

@Builder
final class GoalGetters {

  @Goal
  static Getters create(double lenght, double width, double height) {
    return new Getters(lenght, width, height);
  }

}
