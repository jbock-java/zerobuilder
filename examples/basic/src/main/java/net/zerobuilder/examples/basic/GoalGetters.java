package net.zerobuilder.examples.basic;

import net.zerobuilder.Build;

@Build
class GoalGetters {

  static Getters create(double lenght, double width, double height) {
    return new Getters(lenght, width, height);
  }

}
