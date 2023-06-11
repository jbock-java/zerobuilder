package net.zerobuilder.examples.values;

import net.zerobuilder.Builder;
import net.zerobuilder.Updater;

// null checking
public class SimpleNull {

  static final class BasicNull {
    final String string;

    @Builder
    @Updater
    BasicNull(String string) {
      this.string = string;
    }
  }

}
