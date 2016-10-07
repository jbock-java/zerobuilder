package net.zerobuilder.examples.values;

import net.zerobuilder.Builders;
import net.zerobuilder.Goal;
import net.zerobuilder.Step;

import static net.zerobuilder.NullPolicy.REJECT;

// null checking
public class SimpleNull {

  @Builders
  static final class BasicNull {
    final String string;

    @Goal(toBuilder = true)
    BasicNull(@Step(nullPolicy = REJECT) String string) {
      this.string = string;
    }
  }

}
