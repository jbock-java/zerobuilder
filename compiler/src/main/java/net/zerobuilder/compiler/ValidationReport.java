package net.zerobuilder.compiler;

import com.google.common.base.Optional;

import javax.annotation.processing.Messager;
import javax.lang.model.element.Element;

import static javax.tools.Diagnostic.Kind.ERROR;

final class ValidationReport<T extends Element> {

  private final Optional<String> message;
  private final T element;

  private ValidationReport(Optional<String> message, T element) {
    this.message = message;
    this.element = element;
  }

  boolean isClean() {
    return !message.isPresent();
  }

  void printMessagesTo(Messager messager) {
    if (message.isPresent()) {
      messager.printMessage(ERROR, message.get(), element);
    }
  }

  static <T extends Element> Builder about(T element) {
    return new Builder(element);
  }

  static final class Builder<T extends Element> {

    private final T element;

    private Builder(T element) {
      this.element = element;
    }

    ValidationReport error(String message) {
      return new ValidationReport(Optional.of(message), element);
    }

    ValidationReport clean() {
      return new ValidationReport(Optional.<String>absent(), element);
    }

  }
}
