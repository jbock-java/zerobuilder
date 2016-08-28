package net.zerobuilder.compiler;

import com.google.common.collect.ImmutableList;

import javax.lang.model.element.ExecutableElement;
import javax.lang.model.element.NestingKind;
import javax.lang.model.element.TypeElement;
import java.util.EnumSet;

import static javax.lang.model.element.Modifier.PRIVATE;
import static javax.lang.model.element.Modifier.STATIC;
import static javax.lang.model.element.NestingKind.MEMBER;
import static javax.lang.model.element.NestingKind.TOP_LEVEL;

final class TypeValidator {

  private final EnumSet<NestingKind> allowedNestingKinds = EnumSet.of(TOP_LEVEL, MEMBER);

  ValidationReport<TypeElement, ?> validateElement(TypeElement element, ImmutableList<ExecutableElement> targetMethods) {
    ValidationReport.Builder<TypeElement, ?> builder = ValidationReport.about(element, Object.class);
    if (targetMethods.isEmpty()) {
      return builder.error(Messages.ErrorMessages.METHOD_NOT_FOUND);
    }
    if (targetMethods.size() > 1) {
      return builder.error(Messages.ErrorMessages.SEVERAL_METHODS);
    }
    if (element.getModifiers().contains(PRIVATE)) {
      return builder.error(Messages.ErrorMessages.PRIVATE_CLASS);
    }
    if (!allowedNestingKinds.contains(element.getNestingKind())
        || (element.getNestingKind() == MEMBER
        && !element.getModifiers().contains(STATIC))) {
      return builder.error(Messages.ErrorMessages.NESTING_KIND);
    }
    return builder.clean();
  }

}
