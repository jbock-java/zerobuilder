package net.zerobuilder.compiler.analyse;

import com.google.common.base.Joiner;
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

  /**
   * <p>If {@code type} is a top level class, this returns a class in the same package,
   * with class name {@code type + suffix}.
   * </p><p>
   * If {@code type} is nested, a top level class
   * name derived from its name and nested parents is used instead.</p>
   *
   * @param type   A type name
   * @param suffix A string that usually starts with an uppercase character
   * @return A top level type in the same package.
   */
  static ClassName appendSuffix(ClassName type, String suffix) {
    String name = Joiner.on('_').join(type.simpleNames()) + suffix;
    return type.topLevelClassName().peerClass(name);
  }

  private Utilities() {
    throw new UnsupportedOperationException("no instances");
  }
}
