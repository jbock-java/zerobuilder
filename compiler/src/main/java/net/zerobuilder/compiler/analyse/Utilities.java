package net.zerobuilder.compiler.analyse;

import com.google.common.collect.ImmutableSet;
import com.squareup.javapoet.ClassName;

import java.util.Collection;

import static com.google.common.base.CaseFormat.LOWER_CAMEL;
import static com.google.common.base.CaseFormat.UPPER_CAMEL;
import static java.lang.Character.isUpperCase;

final class Utilities {

  static final class ClassNames {

    static final ClassName COLLECTION = ClassName.get(Collection.class);

    private ClassNames() {
      throw new UnsupportedOperationException("no instances");
    }
  }

  private static final ImmutableSet<String> reservedWords = ImmutableSet.of(
      "abstract", "continue", "for", "new", "switch", "assert", "default", "if", "package",
      "synchronized", "boolean", "do", "goto", "private", "this", "break", "double", "implements",
      "protected", "throw", "byte", "else", "import", "public", "throws", "case", "enum",
      "instanceof", "return", "transient", "catch", "extends", "int", "short", "try", "char", "final",
      "interface", "static", "void", "class", "finally", "long", "strictfp", "volatile", "const",
      "float", "native", "super", "while");

  static String upcase(String s) {
    return LOWER_CAMEL.to(UPPER_CAMEL, s);
  }

  static String downcase(String s) {
    if (s.length() >= 2 && isUpperCase(s.charAt(1))) {
      return s;
    }
    String lowered = UPPER_CAMEL.to(LOWER_CAMEL, s);
    if (reservedWords.contains(lowered)) {
      return s;
    }
    return lowered;
  }

  private Utilities() {
    throw new UnsupportedOperationException("no instances");
  }
}
