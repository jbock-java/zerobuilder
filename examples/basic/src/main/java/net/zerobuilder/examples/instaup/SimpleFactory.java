package net.zerobuilder.examples.instaup;

import net.zerobuilder.Builders;
import net.zerobuilder.Goal;

import java.io.IOException;

@Builders(recycle = true)
final class SimpleFactory {
  private final String string;

  SimpleFactory(String string) {
    this.string = string;
  }

  @Goal(builder = false, updater = true)
  Simple simple(String appendix) throws IOException {
    return new Simple(string, appendix);
  }
}
