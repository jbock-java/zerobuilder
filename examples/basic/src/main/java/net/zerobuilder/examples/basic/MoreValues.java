package net.zerobuilder.examples.basic;

import net.zerobuilder.Builders;
import net.zerobuilder.Goal;

public class MoreValues {

  // goal name is a reserved word
  @Builders
  static class Interface {
    final String foo;
    @Goal(name = "interface")
    Interface(String foo) {
      this.foo = foo;
    }
  }

}
