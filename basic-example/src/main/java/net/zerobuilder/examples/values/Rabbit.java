package net.zerobuilder.examples.values;

import net.zerobuilder.Builder;

final class Rabbit {

  final String name;

  @Builder
  Rabbit(String name) {
    this.name = name;
  }

}
