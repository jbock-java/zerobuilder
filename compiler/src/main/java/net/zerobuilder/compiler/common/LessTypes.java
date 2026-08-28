package net.zerobuilder.compiler.common;

import javax.lang.model.element.Element;
import javax.lang.model.element.ElementVisitor;
import javax.lang.model.element.TypeElement;
import javax.lang.model.type.DeclaredType;
import javax.lang.model.type.ErrorType;
import javax.lang.model.type.TypeMirror;
import javax.lang.model.type.TypeVariable;
import javax.lang.model.type.TypeVisitor;
import javax.lang.model.util.SimpleElementVisitor14;
import javax.lang.model.util.SimpleTypeVisitor14;

public final class LessTypes {

  private static final ElementVisitor<TypeElement, Void> TYPE_ELEMENT_VISITOR =
      new SimpleElementVisitor14<>() {
        @Override
        protected TypeElement defaultAction(Element e, Void p) {
          throw new IllegalArgumentException();
        }

        @Override
        public TypeElement visitType(TypeElement e, Void p) {
          return e;
        }
      };

  private static final TypeVisitor<Element, Void> AS_ELEMENT_VISITOR =
      new SimpleTypeVisitor14<>() {
        @Override
        protected Element defaultAction(TypeMirror e, Void p) {
          return null;
        }

        @Override
        public Element visitDeclared(DeclaredType t, Void p) {
          return t.asElement();
        }

        @Override
        public Element visitError(ErrorType t, Void p) {
          return t.asElement();
        }

        @Override
        public Element visitTypeVariable(TypeVariable t, Void p) {
          return t.asElement();
        }
      };

  public static TypeElement asTypeElement(TypeMirror mirror) {
    Element element = mirror.accept(AS_ELEMENT_VISITOR, null);
    if (element == null) {
      return null;
    }
    return element.accept(TYPE_ELEMENT_VISITOR, null);
  }

  private LessTypes() {
  }
}
