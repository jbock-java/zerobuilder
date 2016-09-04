package net.zerobuilder.compiler;

import javax.lang.model.element.Element;

final class ValidationException extends Exception {
  private static final long serialVersionUID = 0;
  final Element about;
  ValidationException(String message, Element about) {
    super(message);
    this.about = about;
  }
}
