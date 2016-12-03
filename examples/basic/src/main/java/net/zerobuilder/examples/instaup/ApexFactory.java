package net.zerobuilder.examples.instaup;

import net.zerobuilder.Builder;
import net.zerobuilder.Updater;

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
}
