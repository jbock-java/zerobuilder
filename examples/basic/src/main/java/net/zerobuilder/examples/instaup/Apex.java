package net.zerobuilder.examples.instaup;

import java.sql.SQLException;

final class Apex<S extends String> {
  private final String string;
  private final S appendix;

  S appendix() throws SQLException {
    return appendix;
  }

  Apex(String string, S appendix) {
    this.string = string;
    this.appendix = appendix;
  }

  String concat() {
    return string + appendix;
  }
}
