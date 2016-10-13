package net.zerobuilder.examples.values;

import net.zerobuilder.Builders;
import net.zerobuilder.Goal;

import java.io.IOException;

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

  // goal returns void
  @Builders(recycle = true)
  static class Nothing {
    @Goal(name = "append")
    static void append(StringBuilder sb, String word) {
      sb.append(word);
    }
  }

  // goal returns primitive
  @Builders(recycle = true)
  static class Sum {
    @Goal(name = "sum")
    static int sum(int a, int b) {
      return a + b;
    }
  }

  // projection method declares exception
  @Builders(recycle = true)
  static final class NothingSpecial {
    private final String foo;

    @Goal(updater = true)
    NothingSpecial(String foo) {
      this.foo = foo;
    }

    String foo() throws IOException {
      return foo;
    }
  }
}
