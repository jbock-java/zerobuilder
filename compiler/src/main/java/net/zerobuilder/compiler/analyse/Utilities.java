package net.zerobuilder.compiler.analyse;

import com.palantir.javapoet.ClassName;
import com.palantir.javapoet.TypeName;
import java.util.List;
import javax.lang.model.element.ExecutableElement;

import static java.util.stream.Collectors.toList;

final class Utilities {

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
  static ClassName peer(ClassName type, String suffix) {
    String name = String.join("_", type.simpleNames()) + suffix;
    return type.topLevelClassName().peerClass(name);
  }

  static List<TypeName> thrownTypes(ExecutableElement executableElement) {
    return executableElement.getThrownTypes().stream()
        .map(TypeName::get)
        .collect(toList());
  }

  private Utilities() {
  }
}
