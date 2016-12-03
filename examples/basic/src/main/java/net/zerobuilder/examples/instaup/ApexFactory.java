package net.zerobuilder.examples.instaup;

import net.zerobuilder.Builder;
import net.zerobuilder.Updater;

import java.io.IOException;

final class ApexFactory<T extends String> {
  private final T string;

  ApexFactory(T string) {
    this.string = string;
  }

  @Builder
  @Updater
  <S extends String> Apex<S> apex(S appendix) throws IOException {
    return new Apex(string, appendix);
  }
}
