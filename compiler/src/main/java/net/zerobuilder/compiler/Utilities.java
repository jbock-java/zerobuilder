package net.zerobuilder.compiler;

import com.google.common.collect.ImmutableSet;
import com.squareup.javapoet.ClassName;
import com.squareup.javapoet.CodeBlock;
import com.squareup.javapoet.ParameterSpec;
import com.squareup.javapoet.TypeName;

import java.util.Arrays;
import java.util.Collection;
import java.util.List;

import static com.google.common.base.CaseFormat.LOWER_CAMEL;
import static com.google.common.base.CaseFormat.UPPER_CAMEL;
import static java.lang.Character.isUpperCase;

public final class Utilities {

  public static final class ClassNames {

    public static final ClassName OBJECT = ClassName.get(Object.class);
    public static final ClassName COLLECTION = ClassName.get(Collection.class);
    public static final ClassName ITERABLE = ClassName.get(Iterable.class);

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

  public static final CodeBlock emptyCodeBlock = CodeBlock.of("");

  public static String upcase(String s) {
    return LOWER_CAMEL.to(UPPER_CAMEL, s);
  }

  public static String downcase(String s) {
    if (s.length() >= 2 && isUpperCase(s.charAt(1))) {
      return s;
    }
    String lowered = UPPER_CAMEL.to(LOWER_CAMEL, s);
    if (reservedWords.contains(lowered)) {
      return s;
    }
    return lowered;
  }

  public static CodeBlock statement(String format, Object... args) {
    return CodeBlock.builder().addStatement(format, args).build();
  }

  public static ParameterSpec parameterSpec(TypeName type, String name) {
    return ParameterSpec.builder(type, name).build();
  }

  public static CodeBlock nullCheck(String varName, String message) {
    return CodeBlock.builder()
        .beginControlFlow("if ($N == null)", varName)
        .addStatement("throw new $T($S)", NullPointerException.class, message)
        .endControlFlow().build();
  }

  public static CodeBlock nullCheck(ParameterSpec parameterSpec) {
    return nullCheck(parameterSpec.name, parameterSpec.name);
  }

  public static CodeBlock nullCheck(ParameterSpec parameterSpec, String message) {
    return nullCheck(parameterSpec.name, message);
  }

  public static String distinctFrom(String string, String other) {
    if (!string.equals(other)) {
      return string;
    }
    return 'a' + upcase(string);
  }

  private Utilities() {
    throw new UnsupportedOperationException("no instances");
  }
}
