package net.zerobuilder.examples.instaup;

import net.zerobuilder.Recycle;
import net.zerobuilder.Updater;

import java.io.IOException;

final class SimpleFactory {
  private final String string;

  SimpleFactory(String string) {
    this.string = string;
  }

  @Updater
  @Recycle
  Simple simple(String appendix) throws IOException {
    return new Simple(string, appendix);
  }
}
