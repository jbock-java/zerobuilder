package net.zerobuilder.compiler.analyse;

import com.palantir.javapoet.ClassName;
import com.palantir.javapoet.TypeName;

import javax.lang.model.element.ExecutableElement;
import java.util.List;

final class Utilities {

  /**
   * <p>If {@code type} is a top level class, this returns a class in the same package,
   * with class name {@code type + suffix}.
   * </p><p>
   * If {@code type} is nested, the name of the peer class also reflects
   * the parent class/es.</p>
   *
   * @param type   A type name
   * @param suffix A suffix; should be upper case
   * @return       A top level type in the same package.
   */
  static ClassName peer(ClassName type, String suffix) {
    String name = String.join("_", type.simpleNames()) + suffix;
    return type.topLevelClassName().peerClass(name);
  }

  static List<TypeName> thrownTypes(ExecutableElement executableElement) {
    return executableElement.getThrownTypes().stream()
        .map(TypeName::get)
        .toList();
  }

  private Utilities() {
  }
}
