package isobuilder.compiler;

import com.google.common.base.Optional;
import com.squareup.javapoet.ClassName;

import javax.annotation.processing.Messager;
import javax.lang.model.element.Element;

import static com.google.common.base.Preconditions.checkNotNull;
import static javax.tools.Diagnostic.Kind.ERROR;

final class SourceFileGenerationException extends Exception {
  private final Optional<? extends Element> associatedElement;

  SourceFileGenerationException(
      Optional<ClassName> generatedClassName,
      Throwable cause,
      Optional<? extends Element> associatedElement) {
    super(createMessage(generatedClassName, cause.getMessage()), cause);
    this.associatedElement = checkNotNull(associatedElement);
  }

  private static String createMessage(Optional<ClassName> generatedClassName, String message) {
    return String.format("Could not generate %s: %s.",
        generatedClassName.isPresent()
            ? generatedClassName.get()
            : "unknown file",
        message);
  }

  void printMessageTo(Messager messager) {
    if (associatedElement.isPresent()) {
      messager.printMessage(ERROR, getMessage(), associatedElement.get());
    } else {
      messager.printMessage(ERROR, getMessage());
    }
  }
}
