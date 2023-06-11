package net.zerobuilder.examples.instaup;

import net.zerobuilder.Builder;
import net.zerobuilder.Updater;

import java.sql.SQLException;

final class ApexFactory<T extends String> {
  private final T string;

  ApexFactory(T string) {
    this.string = string;
  }

  @Updater
  @Builder
  <S extends String> Apex<S> apex(S appendix) {
    return new Apex(string, appendix);
  }
  static final class Apex<S extends String> {
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
}
