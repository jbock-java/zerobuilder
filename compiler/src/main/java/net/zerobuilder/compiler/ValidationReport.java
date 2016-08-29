package net.zerobuilder.compiler;

import com.google.common.base.Optional;

import javax.annotation.processing.Messager;
import javax.lang.model.element.Element;

import static javax.tools.Diagnostic.Kind.ERROR;

final class ValidationReport<T extends Element, P> {

  private final Optional<String> message;
  private final T element;

  final Optional<P> payload;

  private ValidationReport(Optional<String> message, T element, Optional<P> payload) {
    this.message = message;
    this.element = element;
    this.payload = payload;
  }

  boolean isClean() {
    return !message.isPresent();
  }

  void printMessagesTo(Messager messager) {
    if (message.isPresent()) {
      messager.printMessage(ERROR, message.get(), element);
    }
  }

  @SuppressWarnings("unused")
  static <T extends Element, P> Builder about(T element, Class<P> payloadClass) {
    return new Builder(element);
  }

  static final class Builder<T extends Element, P> {

    private final T element;

    private Builder(T element) {
      this.element = element;
    }

    ValidationReport<T, P> error(String message) {
      return new ValidationReport(Optional.of(message), element, Optional.<P>absent());
    }

    ValidationReport<T, P> clean() {
      return new ValidationReport(Optional.<String>absent(), element, Optional.<P>absent());
    }

    ValidationReport<T, P> clean(P payload) {
      return new ValidationReport(Optional.<String>absent(), element, Optional.of(payload));
    }

  }
}
