package net.zerobuilder.examples.instaup;

import net.zerobuilder.Builders;
import net.zerobuilder.Goal;

import java.io.IOException;

@Builders
final class ApexFactory<T extends String> {
  private final T string;

  ApexFactory(T string) {
    this.string = string;
  }

  @Goal(builder = false, updater = true)
  <S extends String> Apex<S> apex(S appendix) throws IOException {
    return new Apex(string, appendix);
  }
}
