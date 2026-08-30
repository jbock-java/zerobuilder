package net.zerobuilder.compiler.analyse;

import javax.lang.model.element.Element;
import javax.tools.Diagnostic;

import static javax.tools.Diagnostic.Kind.ERROR;

public final class ValidationException extends RuntimeException {
  public final Diagnostic.Kind kind;
  public final Element about;

  ValidationException(String message, Element about) {
    super(message);
    this.kind = ERROR;
    this.about = about;
  }
}
