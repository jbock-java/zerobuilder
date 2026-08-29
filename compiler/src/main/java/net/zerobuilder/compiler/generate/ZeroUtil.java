package net.zerobuilder.compiler.generate;

import com.palantir.javapoet.ClassName;
import com.palantir.javapoet.CodeBlock;
import com.palantir.javapoet.FieldSpec;
import com.palantir.javapoet.ParameterSpec;
import com.palantir.javapoet.ParameterizedTypeName;
import com.palantir.javapoet.TypeName;
import com.palantir.javapoet.TypeVariableName;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Set;
import javax.lang.model.element.Modifier;

import static java.lang.Character.isLowerCase;
import static java.lang.Character.isUpperCase;
import static java.lang.Character.toLowerCase;
import static java.lang.Character.toUpperCase;
import static java.util.Collections.nCopies;

public final class ZeroUtil {

  private static final Set<String> RESERVED_WORDS = Set.of(
      "abstract", "assert", "boolean", "break", "byte", "case", "catch", "char", "class",
      "const", "continue", "default", "do", "double", "else", "enum", "extends", "final",
      "finally", "float", "for", "goto", "if", "implements", "import", "instanceof", "int",
      "interface", "long", "native", "new", "package", "private", "protected", "public",
      "return", "short", "static", "strictfp", "super", "switch", "synchronized", "this",
      "throw", "throws", "transient", "try", "void", "volatile", "while");

  public static String upcase(String s) {
    if (s.isEmpty() || isUpperCase(s.charAt(0))) {
      return s;
    }
    return toUpperCase(s.charAt(0)) + s.substring(1);
  }

  public static String downcase(String s) {
    if (s.isEmpty() || isLowerCase(s.charAt(0))) {
      return s;
    }
    if (s.length() >= 2 && isUpperCase(s.charAt(1))) {
      return s;
    }
    String lowered = toLowerCase(s.charAt(0)) + s.substring(1);
    if (RESERVED_WORDS.contains(lowered)) {
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

  public static FieldSpec fieldSpec(TypeName type, String name, Modifier... modifiers) {
    return FieldSpec.builder(type, name, modifiers).build();
  }

  public static String simpleName(TypeName type) {
    if (type.isPrimitive() || type == TypeName.VOID) {
      return ((ClassName) type.box()).simpleName();
    }
    if (type instanceof ClassName) {
      return ((ClassName) type).simpleName();
    }
    if (type instanceof ParameterizedTypeName) {
      return ((ParameterizedTypeName) type).rawType().simpleName();
    }
    throw new IllegalArgumentException("unknown kind: " + type);
  }

  static <E> int[] createRanking(E[] a, E[] b) {
    if (a.length != b.length) {
      throw new IllegalArgumentException("a.length != b.length");
    }
    int[] pos = new int[a.length];
    for (int i = 0; i < a.length; i++) {
      if (b[i].equals(a[i])) {
        pos[i] = i;
      } else {
        pos[i] = indexOf(b, a[i]);
      }
    }
    return pos;
  }

  private static <E> int indexOf(E[] b, E el) {
    for (int i = 0; i < b.length; i++) {
      if (b[i].equals(el)) {
        return i;
      }
    }
    throw new IllegalArgumentException("not found: " + el);
  }

  static <E> List<E> applyRanking(int[] ranking, List<E> input) {
    List<E> result = new ArrayList<>(nCopies(input.size(), null));
    for (int i = 0; i < input.size(); i++) {
      result.set(ranking[i], input.get(i));
    }
    return result;
  }

  public static TypeName parameterizedTypeName(ClassName raw, List<TypeVariableName> typeVars) {
    if (typeVars.isEmpty()) {
      return raw;
    }
    return ParameterizedTypeName.get(raw, typeVars.toArray(new TypeVariableName[0]));
  }

  public static Modifier[] addModifier(Modifier modifier, Modifier[] modifiers) {
    for (Modifier m : modifiers) {
      if (m == modifier) {
        return modifiers;
      }
    }
    Modifier[] copy = Arrays.copyOf(modifiers, modifiers.length + 1);
    copy[modifiers.length] = modifier;
    return copy;
  }

  private ZeroUtil() {
  }
}
