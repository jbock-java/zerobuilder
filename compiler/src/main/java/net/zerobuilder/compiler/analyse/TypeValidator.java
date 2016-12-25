package net.zerobuilder.compiler.analyse;


import javax.lang.model.element.Modifier;
import javax.lang.model.element.NestingKind;
import javax.lang.model.element.TypeElement;
import java.util.EnumSet;
import java.util.Set;

import static javax.lang.model.element.Modifier.PRIVATE;
import static javax.lang.model.element.Modifier.STATIC;
import static javax.lang.model.element.NestingKind.MEMBER;
import static javax.lang.model.element.NestingKind.TOP_LEVEL;
import static net.zerobuilder.compiler.Messages.ErrorMessages.NESTING_KIND;

final class TypeValidator {

  private static final Set<NestingKind> ALLOWED_NESTING_KINDS
      = EnumSet.of(TOP_LEVEL, MEMBER);

  static void validateContextClass(TypeElement type) throws ValidationException {
    Set<Modifier> modifiers = type.getModifiers();
    NestingKind nestingKind = type.getNestingKind();
    if (modifiers.contains(PRIVATE)) {
      throw new ValidationException(NESTING_KIND, type);
    }
    if (!ALLOWED_NESTING_KINDS.contains(nestingKind)
        || nestingKind == MEMBER && !modifiers.contains(STATIC)) {
      throw new ValidationException(NESTING_KIND, type);
    }
  }

  private TypeValidator() {
    throw new UnsupportedOperationException("no instances");
  }
}
