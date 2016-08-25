package isobuilder.examples.kompliziert;

import isobuilder.Builder;

final class SimpleBob {

  private final String kevin;
  private final String chantal;
  private final String justin;

  private SimpleBob(String kevin, String chantal, String justin) {
    this.kevin = kevin;
    this.chantal = chantal;
    this.justin = justin;
  }

  @Builder
  static SimpleBob create(String kevin, String chantal, String justin) {
    return new SimpleBob(kevin, chantal, justin);
  }

}
