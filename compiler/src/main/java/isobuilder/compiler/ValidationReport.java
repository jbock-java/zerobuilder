package isobuilder.compiler;

import com.google.common.base.Optional;

import javax.annotation.processing.Messager;
import javax.lang.model.element.ExecutableElement;

import static javax.tools.Diagnostic.Kind.ERROR;

final class ValidationReport {

  private final Optional<String> message;
  private final ExecutableElement element;

  private ValidationReport(Optional<String> message, ExecutableElement element) {
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

  static Builder about(ExecutableElement element) {
    return new Builder(element);
  }

  static final class Builder {

    private final ExecutableElement element;

    private Builder(ExecutableElement element) {
      this.element = element;
    }

    ValidationReport addError(String message) {
      return new ValidationReport(Optional.of(message), element);
    }

    ValidationReport build() {
      return new ValidationReport(Optional.<String>absent(), element);
    }

  }
}
