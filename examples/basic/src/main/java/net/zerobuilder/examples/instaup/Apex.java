package net.zerobuilder.examples.instaup;

final class Apex<S extends String> {
  private final String string;
  final S appendix;

  Apex(String string, S appendix) {
    this.string = string;
    this.appendix = appendix;
  }
}
