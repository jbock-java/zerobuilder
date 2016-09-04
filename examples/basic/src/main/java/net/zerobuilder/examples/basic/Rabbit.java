package net.zerobuilder.examples.basic;

import net.zerobuilder.Build;
import net.zerobuilder.Goal;

@Build
final class Rabbit {

  final String name;

  @Goal(name = "hare", toBuilder = true)
  Rabbit(String name) {
    this.name = name;
  }

}
