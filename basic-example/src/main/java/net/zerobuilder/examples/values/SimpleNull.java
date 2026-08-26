package net.zerobuilder.examples.values;

import net.zerobuilder.Builder;

// null checking
public class SimpleNull {

  static final class BasicNull {
    final String string;

    @Builder
    BasicNull(String string) {
      this.string = string;
    }
  }

}
