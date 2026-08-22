package net.zerobuilder.examples.values;

import net.zerobuilder.Builder;
import net.zerobuilder.GoalName;

class MoreValues {

  // goal name is a reserved word
  static class Interface {

    final String foo;

    @Builder
    @GoalName("interface")
    Interface(String foo) {
      this.foo = foo;
    }
  }
}
