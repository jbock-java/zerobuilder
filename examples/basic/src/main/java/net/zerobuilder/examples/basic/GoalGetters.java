package net.zerobuilder.examples.basic;

import net.zerobuilder.Build;

@Build(goal = Getters.class, toBuilder = false)
class GoalGetters {

  @Build.Via
  static Getters create(double lenght, double width, double height) {
    return new Getters(lenght, width, height);
  }

}
