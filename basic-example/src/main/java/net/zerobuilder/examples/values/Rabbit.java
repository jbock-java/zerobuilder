package net.zerobuilder.examples.values;

import net.zerobuilder.Builder;
import net.zerobuilder.GoalName;
import net.zerobuilder.Updater;

// changing goal name
final class Rabbit {

  final String name;

  @Builder
  @Updater
  @GoalName("hare")
  Rabbit(String name) {
    this.name = name;
  }

}
