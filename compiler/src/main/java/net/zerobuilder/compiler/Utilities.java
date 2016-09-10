package net.zerobuilder.compiler;

import static com.google.common.base.CaseFormat.LOWER_CAMEL;
import static com.google.common.base.CaseFormat.UPPER_CAMEL;

final class Utilities {

  static String upcase(String s) {
    return LOWER_CAMEL.to(UPPER_CAMEL, s);
  }

  static String downcase(String s) {
    return UPPER_CAMEL.to(LOWER_CAMEL, s);
  }

  private Utilities() {
  }

}
