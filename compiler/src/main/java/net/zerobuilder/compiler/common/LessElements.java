package net.zerobuilder.compiler.common;

import javax.lang.model.element.*;
import javax.lang.model.util.SimpleElementVisitor14;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.function.Predicate;

import static javax.lang.model.element.ElementKind.FIELD;
import static javax.lang.model.element.ElementKind.METHOD;
import static javax.lang.model.element.Modifier.PRIVATE;
import static javax.lang.model.element.Modifier.STATIC;
import static javax.lang.model.util.ElementFilter.fieldsIn;
import static javax.lang.model.util.ElementFilter.methodsIn;

/**
 * Guava-free versions of some helpers from auto-common.
 */
public final class LessElements {

  private static final ElementVisitor<ExecutableElement, Void> EXECUTABLE_ELEMENT_VISITOR =
      new SimpleElementVisitor14<>() {
        @Override
        protected ExecutableElement defaultAction(Element e, Void p) {
          throw new IllegalArgumentException();
        }

        @Override
        public ExecutableElement visitExecutable(ExecutableElement e, Void p) {
          return e;
        }
      };

  /**
   * Find all non-static, visible methods that match the predicate, and group by name.
   * In case of name conflict, the first found wins.
   * The iteration order is:
   * <ul>
   * <li>{@code type} first, {@code Object} last</li>
   * <li>concrete types before interfaces</li>
   * </ul>
   * Ideally the {@code predicate} should prevent name conflicts.
   *
   * @param type      type to search
   * @param predicate filter
   * @return methods by name
   */
  public static Map<String, ExecutableElement> getLocalMethods(
      TypeElement type, Predicate<ExecutableElement> predicate) {
    Map<String, ExecutableElement> methods = new LinkedHashMap<>();
    addEnclosedMethods(type, methods, predicate);
    return methods;
  }

  public static Map<String, VariableElement> getLocalFields(
      TypeElement type) {
    Map<String, VariableElement> fields = new LinkedHashMap<>();
    addEnclosedFields(type, fields);
    return fields;
  }

  private static void addEnclosedMethods(
      TypeElement type,
      Map<String, ExecutableElement> methods,
      Predicate<ExecutableElement> predicate) {
    methodsIn(type.getEnclosedElements())
        .stream()
        .filter(method -> method.getKind() == METHOD)
        .filter(method -> !method.getModifiers().contains(STATIC))
        .filter(method -> !method.getModifiers().contains(PRIVATE))
        .filter(predicate)
        .forEach(method -> methods.put(method.getSimpleName().toString(), method));
  }

  private static void addEnclosedFields(
      TypeElement type,
      Map<String, VariableElement> fields) {
    fieldsIn(type.getEnclosedElements()).stream()
        .filter(field -> field.getKind() == FIELD)
        .filter(field -> !field.getModifiers().contains(STATIC))
        .filter(field -> !field.getModifiers().contains(PRIVATE))
        .forEach(field -> fields.put(field.getSimpleName().toString(), field));
  }

  public static ExecutableElement asExecutable(Element element) {
    return element.accept(EXECUTABLE_ELEMENT_VISITOR, null);
  }

  private LessElements() {
  }
}
