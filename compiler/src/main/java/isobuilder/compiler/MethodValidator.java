package isobuilder.compiler;

import com.squareup.javapoet.ClassName;

import javax.lang.model.element.ExecutableElement;
import javax.lang.model.element.TypeElement;

import static isobuilder.compiler.ErrorMessages.NON_STATIC_METHOD;
import static isobuilder.compiler.ErrorMessages.PRIVATE_METHOD;
import static isobuilder.compiler.ErrorMessages.RETURN_TYPE;
import static isobuilder.compiler.ErrorMessages.NOT_ENOUGH_PARAMETERS;
import static javax.lang.model.element.ElementKind.METHOD;
import static javax.lang.model.element.Modifier.PRIVATE;
import static javax.lang.model.element.Modifier.STATIC;

final class MethodValidator {

  ValidationReport validateElement(TypeElement typeElement, ExecutableElement element) {
    ValidationReport.Builder builder = ValidationReport.about(element);
    if (element.getKind() == METHOD) {
      if (!element.getModifiers().contains(STATIC)) {
        return builder.error(NON_STATIC_METHOD);
      }
      if (!ClassName.get(element.getReturnType()).equals(ClassName.get(typeElement))) {
        return builder.error(RETURN_TYPE);
      }
    }
    if (element.getModifiers().contains(PRIVATE)) {
      return builder.error(PRIVATE_METHOD);
    }
    if (element.getParameters().size() < 2) {
      return builder.error(NOT_ENOUGH_PARAMETERS);
    }
    return builder.clean();
  }

}
