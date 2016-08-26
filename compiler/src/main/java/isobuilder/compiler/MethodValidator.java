package isobuilder.compiler;

import javax.lang.model.element.ExecutableElement;
import javax.lang.model.element.NestingKind;
import javax.lang.model.element.TypeElement;
import java.util.EnumSet;

import static com.google.auto.common.MoreTypes.asTypeElement;
import static isobuilder.compiler.ErrorMessages.NESTING_KIND;
import static isobuilder.compiler.ErrorMessages.NON_STATIC_METHOD;
import static isobuilder.compiler.ErrorMessages.PRIVATE_CLASS;
import static isobuilder.compiler.ErrorMessages.PRIVATE_METHOD;
import static isobuilder.compiler.ErrorMessages.TOO_FEW_PARAMETERS;
import static javax.lang.model.element.ElementKind.METHOD;
import static javax.lang.model.element.Modifier.PRIVATE;
import static javax.lang.model.element.Modifier.STATIC;
import static javax.lang.model.element.NestingKind.MEMBER;
import static javax.lang.model.element.NestingKind.TOP_LEVEL;

final class MethodValidator {

  private final EnumSet<NestingKind> allowedNestingKinds = EnumSet.of(TOP_LEVEL, MEMBER);

  ValidationReport validateElement(ExecutableElement element) {
    ValidationReport.Builder builder = ValidationReport.about(element);
    TypeElement enclosingElement = asTypeElement(element.getEnclosingElement().asType());
    if (element.getKind() == METHOD && !element.getModifiers().contains(STATIC)) {
      builder.addError(NON_STATIC_METHOD);
    }
    if (element.getModifiers().contains(PRIVATE)) {
      builder.addError(PRIVATE_METHOD);
    }
    if (enclosingElement.getModifiers().contains(PRIVATE)) {
      builder.addError(PRIVATE_CLASS);
    }
    if (element.getParameters().size() < 2) {
      builder.addError(TOO_FEW_PARAMETERS);
    }
    if (!allowedNestingKinds.contains(enclosingElement.getNestingKind())
        || (enclosingElement.getNestingKind() == MEMBER
        && !enclosingElement.getModifiers().contains(STATIC))) {
      builder.addError(NESTING_KIND);
    }
    return builder.build();
  }

}
