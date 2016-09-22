package net.zerobuilder.examples.basic;

import net.zerobuilder.Builders;
import net.zerobuilder.Goal;

// changing goal name
@Builders
final class Rabbit {

  final String name;

  @Goal(name = "hare", toBuilder = true)
  Rabbit(String name) {
    this.name = name;
  }

}
