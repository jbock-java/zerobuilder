package net.zerobuilder.compiler.analyse;

import com.squareup.javapoet.ClassName;

import java.util.Arrays;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.function.Function;

import static java.lang.Character.isUpperCase;
import static java.util.stream.Collectors.toList;

final class Utilities {

  static final class ClassNames {

    static final ClassName COLLECTION = ClassName.get(Collection.class);

    private ClassNames() {
      throw new UnsupportedOperationException("no instances");
    }
  }

  private static final Set<String> reservedWords = new HashSet<>(Arrays.asList(
      "abstract", "continue", "for", "new", "switch", "assert", "default", "if", "package",
      "synchronized", "boolean", "do", "goto", "private", "this", "break", "double", "implements",
      "protected", "throw", "byte", "else", "import", "public", "throws", "case", "enum",
      "instanceof", "return", "transient", "catch", "extends", "int", "short", "try", "char", "final",
      "interface", "static", "void", "class", "finally", "long", "strictfp", "volatile", "const",
      "float", "native", "super", "while"));

  static String upcase(String s) {
    if (s.isEmpty() || Character.isUpperCase(s.charAt(0))) {
      return s;
    }
    return Character.toUpperCase(s.charAt(0)) + s.substring(1);
  }

  static String downcase(String s) {
    if (s.length() >= 2 && isUpperCase(s.charAt(1))) {
      return s;
    }
    String lowered = Character.toLowerCase(s.charAt(0)) + s.substring(1);
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
    String name = String.join("_", type.simpleNames()) + suffix;
    return type.topLevelClassName().peerClass(name);
  }

  static <X, E> List<E> transform(Collection<X> input, Function<X, E> function) {
    return input.stream().map(function).collect(toList());
  }

  private Utilities() {
    throw new UnsupportedOperationException("no instances");
  }
}