package net.zerobuilder.compiler;

import javax.annotation.processing.Messager;
import javax.lang.model.element.Element;

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
}
