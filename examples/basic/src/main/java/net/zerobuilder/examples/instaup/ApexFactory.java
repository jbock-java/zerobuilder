package net.zerobuilder.examples.instaup;

import net.zerobuilder.Builder;

final class ApexFactory<T extends String> {
  private final T string;

  ApexFactory(T string) {
    this.string = string;
  }

  @Builder
  <S extends String> Apex<S> apex(S appendix) {
    return new Apex(string, appendix);
  }
}
