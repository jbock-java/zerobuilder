package isobuilder.compiler;

import javax.lang.model.element.ExecutableElement;

import static isobuilder.compiler.ErrorMessages.NON_STATIC_METHOD;
import static isobuilder.compiler.ErrorMessages.PRIVATE_METHOD;
import static javax.lang.model.element.Modifier.PRIVATE;
import static javax.lang.model.element.Modifier.STATIC;

public final class BuilderValidator {

  ValidationReport<ExecutableElement> validateMethod(ExecutableElement method) {
    ValidationReport.Builder<ExecutableElement> builder = ValidationReport.about(method);
    if (!method.getModifiers().contains(STATIC)) {
      builder.addError(NON_STATIC_METHOD, method);
    }
    if (method.getModifiers().contains(PRIVATE)) {
      builder.addError(PRIVATE_METHOD, method);
    }
    return builder.build();
  }

}
