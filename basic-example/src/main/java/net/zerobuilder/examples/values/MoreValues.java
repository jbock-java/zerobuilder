package net.zerobuilder.examples.values;

import net.zerobuilder.Builder;

class MoreValues {

  // goal name is a reserved word
  static class Interface {

    final String foo;

    @Builder
    Interface(String foo) {
      this.foo = foo;
    }
  }
}
