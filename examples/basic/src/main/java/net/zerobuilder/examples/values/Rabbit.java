package net.zerobuilder.examples.values;

import net.zerobuilder.Goal;

// changing goal name
final class Rabbit {

  final String name;

  @Goal(name = "hare", updater = true)
  Rabbit(String name) {
    this.name = name;
  }

}
