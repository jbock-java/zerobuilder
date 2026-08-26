package net.zerobuilder.examples.values;

import net.zerobuilder.RecordBuilder;

class MoreValues {

  // goal name is a reserved word
  @RecordBuilder
  static class Interface {

    final String foo;

    Interface(String foo) {
      this.foo = foo;
    }
  }
}
