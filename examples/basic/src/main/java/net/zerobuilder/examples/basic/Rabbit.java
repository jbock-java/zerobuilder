package net.zerobuilder.examples.basic;

import net.zerobuilder.Builder;
import net.zerobuilder.Goal;

@Builder
final class Rabbit {

  final String name;

  @Goal(name = "hare", toBuilder = true)
  Rabbit(String name) {
    this.name = name;
  }

}
