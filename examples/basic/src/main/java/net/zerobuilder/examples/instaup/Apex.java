package net.zerobuilder.examples.instaup;

final class Apex {
  private final String string;
  final String appendix;

  Apex(String string, String appendix) {
    this.string = string;
    this.appendix = appendix;
  }

  String concat() {
    return string + appendix;
  }
}
