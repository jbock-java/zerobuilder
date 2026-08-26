package net.zerobuilder.examples.values;

import net.zerobuilder.RecordBuilder;

@RecordBuilder
final class Rabbit {

  final String name;

  Rabbit(String name) {
    this.name = name;
  }

}
