package net.zerobuilder.compiler;

import com.squareup.javapoet.ClassName;
import net.zerobuilder.compiler.ValidationReport.ReportBuilder;

import javax.lang.model.element.ExecutableElement;
import javax.lang.model.element.TypeElement;

import static net.zerobuilder.compiler.Messages.ErrorMessages.NON_STATIC_METHOD;
import static net.zerobuilder.compiler.Messages.ErrorMessages.NOT_ENOUGH_PARAMETERS;
import static net.zerobuilder.compiler.Messages.ErrorMessages.PRIVATE_METHOD;
import static net.zerobuilder.compiler.Messages.ErrorMessages.RETURN_TYPE;
import static javax.lang.model.element.ElementKind.METHOD;
import static javax.lang.model.element.Modifier.PRIVATE;
import static javax.lang.model.element.Modifier.STATIC;
import static net.zerobuilder.compiler.ValidationReport.about;

final class MethodValidator {

  ValidationReport<TypeElement, ?> validateElement(TypeElement typeElement, ExecutableElement element) {
    ReportBuilder<TypeElement, ?> builder = about(element, Object.class);
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
    if (element.getParameters().size() < 1) {
      return builder.error(NOT_ENOUGH_PARAMETERS);
    }
    return builder.clean();
  }

}
