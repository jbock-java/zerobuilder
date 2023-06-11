package net.zerobuilder.examples.instaup;

import net.zerobuilder.Recycle;
import net.zerobuilder.Updater;

import java.io.IOException;
import java.sql.SQLException;

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
  static final class Simple {
    private final String string;
    private final String appendix;

    String appendix() throws SQLException {
      return appendix;
    }

    Simple(String string, String appendix) {
      this.string = string;
      this.appendix = appendix;
    }

    String concat() {
      return string + appendix;
    }
  }
}
