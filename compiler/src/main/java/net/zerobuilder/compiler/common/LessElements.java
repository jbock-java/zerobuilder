package net.zerobuilder.compiler.common;

import javax.lang.model.element.Element;
import javax.lang.model.element.ElementVisitor;
import javax.lang.model.element.ExecutableElement;
import javax.lang.model.element.Modifier;
import javax.lang.model.element.PackageElement;
import javax.lang.model.element.TypeElement;
import javax.lang.model.type.TypeKind;
import javax.lang.model.type.TypeMirror;
import javax.lang.model.util.SimpleElementVisitor6;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.function.Predicate;

import static javax.lang.model.element.ElementKind.PACKAGE;
import static javax.lang.model.util.ElementFilter.methodsIn;
import static net.zerobuilder.compiler.common.LessTypes.asTypeElement;

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

  public static Collection<ExecutableElement> getLocalAndInheritedMethods(
      TypeElement type, Predicate<ExecutableElement> predicate) {
    Map<String, ExecutableElement> methodMap = new LinkedHashMap<>();
    getLocalAndInheritedMethods(getPackage(type), type, methodMap, predicate);
    return methodMap.values();
  }

  private static void getLocalAndInheritedMethods(
      PackageElement pkg, TypeElement type, Map<String, ExecutableElement> methods,
      Predicate<ExecutableElement> predicate) {
    for (TypeMirror superInterface : type.getInterfaces()) {
      getLocalAndInheritedMethods(pkg, asTypeElement(superInterface), methods,
          predicate);
    }
    if (type.getSuperclass().getKind() != TypeKind.NONE) {
      // Visit the superclass after superinterfaces so we will always see the implementation of a
      // method after any interfaces that declared it.
      getLocalAndInheritedMethods(pkg, asTypeElement(type.getSuperclass()), methods,
          predicate);
    }
    methodsIn(type.getEnclosedElements())
        .stream()
        .filter(predicate).forEach(method -> {
      if (!method.getModifiers().contains(Modifier.STATIC)
          && methodVisibleFromPackage(method, pkg)) {
        methods.computeIfAbsent(method.getSimpleName().toString(), name -> method);
      }
    });
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
