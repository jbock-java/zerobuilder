package net.zerobuilder.examples.values;

import net.zerobuilder.Builder;
import net.zerobuilder.NotNullStep;
import net.zerobuilder.Updater;

// null checking
public class SimpleNull {

  static final class BasicNull {
    final String string;

    @Builder
    @Updater
    BasicNull(@NotNullStep String string) {
      this.string = string;
    }
  }

}
