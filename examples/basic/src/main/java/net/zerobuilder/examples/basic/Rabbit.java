package net.zerobuilder.examples.basic;

import net.zerobuilder.Build;

@Build
final class Rabbit {

  final String name;

  @Build.Goal(value = "hare", toBuilder = true)
  Rabbit(String name) {
    this.name = name;
  }

}
