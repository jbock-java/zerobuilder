package net.zerobuilder.compiler.common;

import javax.lang.model.element.Element;
import javax.lang.model.element.Modifier;
import java.util.Objects;
import java.util.Set;

import static javax.lang.model.element.ElementKind.PACKAGE;

/**
 * Guava-free versions of some helpers from auto-common.
 */
enum Visibility {
  PRIVATE,
  DEFAULT,
  PROTECTED,
  PUBLIC;

  static Visibility ofElement(Element element) {
    Objects.requireNonNull(element);
    if (element.getKind().equals(PACKAGE)) {
      return PUBLIC;
    }
    Set<Modifier> modifiers = element.getModifiers();
    if (modifiers.contains(Modifier.PRIVATE)) {
      return PRIVATE;
    } else if (modifiers.contains(Modifier.PROTECTED)) {
      return PROTECTED;
    } else if (modifiers.contains(Modifier.PUBLIC)) {
      return PUBLIC;
    } else {
      return DEFAULT;
    }
  }
}