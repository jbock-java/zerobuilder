package net.zerobuilder.compiler;

import javax.annotation.processing.Messager;
import javax.lang.model.element.Element;

import static javax.tools.Diagnostic.Kind.ERROR;
import static javax.tools.Diagnostic.Kind.WARNING;

class ValidationException extends Exception {
  private final Element about;
  ValidationException(String message, Element about) {
    super(message);
    this.about = about;
  }
  void printMessage(Messager messager) {
    messager.printMessage(WARNING, getMessage(), about);
  }
  static void checkState(boolean condition, String message, Element about) throws ValidationException {
    if (!condition) {
      throw new ValidationException(message, about);
    }
  }
}
