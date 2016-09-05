package net.zerobuilder.compiler;

import javax.lang.model.element.Element;
import javax.tools.Diagnostic;

import static javax.tools.Diagnostic.Kind.ERROR;

final class ValidationException extends Exception {
  private static final long serialVersionUID = 0;
  final Diagnostic.Kind kind;
  final Element about;
  ValidationException(Diagnostic.Kind kind, String message, Element about) {
    super(message);
    this.kind = kind;
    this.about = about;
  }
  ValidationException(String message, Element about) {
    this(ERROR, message, about);
  }
}
