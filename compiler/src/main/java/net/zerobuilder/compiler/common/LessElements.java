package net.zerobuilder.compiler.common;

import javax.lang.model.element.Element;
import javax.lang.model.element.ElementVisitor;
import javax.lang.model.element.ExecutableElement;
import javax.lang.model.element.Modifier;
import javax.lang.model.element.PackageElement;
import javax.lang.model.element.TypeElement;
import javax.lang.model.type.TypeKind;
import javax.lang.model.type.TypeMirror;
import javax.lang.model.util.ElementFilter;
import javax.lang.model.util.Elements;
import javax.lang.model.util.SimpleElementVisitor6;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

import static javax.lang.model.element.ElementKind.PACKAGE;

/**
 * Guava-free versions of some helpers from auto-common.
 */
public final class LessElements {

  private static final ElementVisitor<TypeElement, Void> TYPE_ELEMENT_VISITOR =
      new SimpleElementVisitor6<TypeElement, Void>() {
        @Override
        protected TypeElement defaultAction(Element e, Void p) {
          throw new IllegalArgumentException();
        }

        @Override
        public TypeElement visitType(TypeElement e, Void p) {
          return e;
        }
      };

  private static final ElementVisitor<ExecutableElement, Void> EXECUTABLE_ELEMENT_VISITOR =
      new SimpleElementVisitor6<ExecutableElement, Void>() {
        @Override
        protected ExecutableElement defaultAction(Element e, Void p) {
          throw new IllegalArgumentException();
        }

        @Override
        public ExecutableElement visitExecutable(ExecutableElement e, Void p) {
          return e;
        }
      };

  public static Set<ExecutableElement> getLocalAndInheritedMethods(
      TypeElement type, Elements elementUtils) {
    Map<String, Set<ExecutableElement>> methodMap = new LinkedHashMap<>();
    getLocalAndInheritedMethods(getPackage(type), type, methodMap);
    // Find methods that are overridden. We do this using `Elements.overrides`, which means
    // that it is inherently a quadratic operation, since we have to compare every method against
    // every other method. We reduce the performance impact by (a) grouping methods by name, since
    // a method cannot override another method with a different name, and (b) making sure that
    // methods in ancestor types precede those in descendant types, which means we only have to
    // check a method against the ones that follow it in that order.
    Set<ExecutableElement> overridden = new LinkedHashSet<ExecutableElement>();
    for (String methodName : methodMap.keySet()) {
      List<ExecutableElement> methodList = new ArrayList(methodMap.get(methodName));
      for (int i = 0; i < methodList.size(); i++) {
        ExecutableElement methodI = methodList.get(i);
        for (int j = i + 1; j < methodList.size(); j++) {
          ExecutableElement methodJ = methodList.get(j);
          if (elementUtils.overrides(methodJ, methodI, type)) {
            overridden.add(methodI);
          }
        }
      }
    }
    Set<ExecutableElement> methods =
        new LinkedHashSet<>(methodMap.values().stream()
            .map(Set::stream)
            .flatMap(Function.identity())
            .collect(Collectors.toSet()));
    methods.removeAll(overridden);
    return methods;
  }

  // Add to `methods` the instance methods from `type` that are visible to code in the
  // package `pkg`. This means all the instance methods from `type` itself and all instance methods
  // it inherits from its ancestors, except private methods and package-private methods in other
  // packages. This method does not take overriding into account, so it will add both an ancestor
  // method and a descendant method that overrides it.
  // `methods` is a multimap from a method name to all of the methods with that name, including
  // methods that override or overload one another. Within those methods, those in ancestor types
  // always precede those in descendant types.
  private static void getLocalAndInheritedMethods(
      PackageElement pkg, TypeElement type, Map<String, Set<ExecutableElement>> methods) {
    for (TypeMirror superInterface : type.getInterfaces()) {
      getLocalAndInheritedMethods(pkg, LessTypes.asTypeElement(superInterface), methods);
    }
    if (type.getSuperclass().getKind() != TypeKind.NONE) {
      // Visit the superclass after superinterfaces so we will always see the implementation of a
      // method after any interfaces that declared it.
      getLocalAndInheritedMethods(pkg, LessTypes.asTypeElement(type.getSuperclass()), methods);
    }
    for (ExecutableElement method : ElementFilter.methodsIn(type.getEnclosedElements())) {
      if (!method.getModifiers().contains(Modifier.STATIC)
          && methodVisibleFromPackage(method, pkg)) {
        if (methods.get(method.getSimpleName().toString()) == null) {
          methods.put(method.getSimpleName().toString(), new HashSet<>());
        }
        methods.get(method.getSimpleName().toString()).add(method);
      }
    }
  }

  private static boolean methodVisibleFromPackage(ExecutableElement method, PackageElement pkg) {
    Visibility visibility = Visibility.ofElement(method);
    switch (visibility) {
      case PRIVATE:
        return false;
      case DEFAULT:
        return getPackage(method).equals(pkg);
      default:
        return true;
    }
  }

  private static PackageElement getPackage(Element element) {
    while (element.getKind() != PACKAGE) {
      element = element.getEnclosingElement();
    }
    return (PackageElement) element;
  }

  public static ExecutableElement asExecutable(Element element) {
    return element.accept(EXECUTABLE_ELEMENT_VISITOR, null);
  }

  static TypeElement asType(Element element) {
    return element.accept(TYPE_ELEMENT_VISITOR, null);
  }

  private LessElements() {
    throw new UnsupportedOperationException("no instances");
  }
}
