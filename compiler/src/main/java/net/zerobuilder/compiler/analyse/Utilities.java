package net.zerobuilder.compiler.analyse;

import io.jbock.javapoet.ClassName;
import io.jbock.javapoet.ParameterizedTypeName;
import io.jbock.javapoet.TypeName;

import javax.lang.model.element.ExecutableElement;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.function.BiConsumer;
import java.util.function.BinaryOperator;
import java.util.function.Function;
import java.util.function.Supplier;
import java.util.stream.Collector;

import static java.lang.Character.isUpperCase;
import static java.util.Collections.emptySet;
import static java.util.stream.Collectors.toList;

final class Utilities {

  static final class ClassNames {

    static final ClassName COLLECTION = ClassName.get(Collection.class);

    private ClassNames() {
      throw new UnsupportedOperationException("no instances");
    }
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
  static ClassName peer(ClassName type, String suffix) {
    String name = String.join("_", type.simpleNames()) + suffix;
    return type.topLevelClassName().peerClass(name);
  }

  static <E> List<E> sortedCopy(List<E> input, Comparator<E> comparator) {
    ArrayList<E> sorted = new ArrayList<>(input.size());
    sorted.addAll(input);
    sorted.sort(comparator);
    return sorted;
  }

  static <K> Optional<K> findKey(Map<K, ?> map, List<K> keys) {
    for (K key : keys) {
      if (map.containsKey(key)) {
        return Optional.of(key);
      }
    }
    return Optional.empty();
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
