package isobuilder.compiler;

import com.google.common.collect.ImmutableList;

import javax.lang.model.element.ExecutableElement;
import javax.lang.model.element.NestingKind;
import javax.lang.model.element.TypeElement;
import java.util.EnumSet;

import static isobuilder.compiler.ErrorMessages.METHOD_NOT_FOUND;
import static isobuilder.compiler.ErrorMessages.NESTING_KIND;
import static isobuilder.compiler.ErrorMessages.PRIVATE_CLASS;
import static isobuilder.compiler.ErrorMessages.SEVERAL_METHODS;
import static javax.lang.model.element.Modifier.PRIVATE;
import static javax.lang.model.element.Modifier.STATIC;
import static javax.lang.model.element.NestingKind.MEMBER;
import static javax.lang.model.element.NestingKind.TOP_LEVEL;

final class TypeValidator {

  private final EnumSet<NestingKind> allowedNestingKinds = EnumSet.of(TOP_LEVEL, MEMBER);

  ValidationReport validateElement(TypeElement enclosingElement, ImmutableList<ExecutableElement> targetMethods) {
    ValidationReport.Builder<TypeElement> builder = ValidationReport.about(enclosingElement);
    if (targetMethods.isEmpty()) {
      return builder.error(METHOD_NOT_FOUND);
    }
    if (targetMethods.size() > 1) {
      return builder.error(SEVERAL_METHODS);
    }
    if (enclosingElement.getModifiers().contains(PRIVATE)) {
      return builder.error(PRIVATE_CLASS);
    }
    if (!allowedNestingKinds.contains(enclosingElement.getNestingKind())
        || (enclosingElement.getNestingKind() == MEMBER
        && !enclosingElement.getModifiers().contains(STATIC))) {
      return builder.error(NESTING_KIND);
    }
    return builder.clean();
  }

}
