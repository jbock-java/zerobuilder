package net.zerobuilder.compiler.analyse;

import com.squareup.javapoet.ClassName;
import com.squareup.javapoet.TypeName;

import javax.lang.model.element.ExecutableElement;
import java.util.Collection;
import java.util.List;

import static java.util.stream.Collectors.toList;

final class Utilities {

  static final class ClassNames {

    static final ClassName COLLECTION = ClassName.get(Collection.class);

    private ClassNames() {
      throw new UnsupportedOperationException("no instances");
    }
  }

  static List<TypeName> thrownTypes(ExecutableElement executableElement) {
    return executableElement.getThrownTypes().stream()
        .map(TypeName::get)
        .collect(toList());
  }

  private Utilities() {
    throw new UnsupportedOperationException("no instances");
  }
}
