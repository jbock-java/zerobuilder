package net.zerobuilder.examples.instaup;

import java.sql.SQLException;

final class Simple {
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
