package net.zerobuilder.examples.values;

import net.zerobuilder.Builder;
import net.zerobuilder.GoalName;
import net.zerobuilder.Recycle;

import java.io.IOException;

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

  // goal returns void
  static class Nothing {

    @Builder
    @Recycle
    @GoalName("append")
    static void append(StringBuilder sb, String word) {
      sb.append(word);
    }
  }

  // goal returns primitive
  static class Sum {

    @Builder
    @Recycle
    @GoalName("sum")
    static int sum(int a, int b) {
      return a + b;
    }
  }

  // projection method declares exception
  static final class NothingSpecial {
    private final String foo;

    @Builder
    @Recycle
    @GoalName("append")
    NothingSpecial(String foo) {
      this.foo = foo;
    }

    String foo() throws IOException {
      return foo;
    }
  }
}
