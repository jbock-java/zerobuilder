package net.zerobuilder.compiler.analyse;

import com.google.common.collect.ImmutableSet;

import javax.lang.model.element.Modifier;
import javax.lang.model.element.NestingKind;
import javax.lang.model.element.TypeElement;
import java.util.Set;

import static com.google.common.collect.Sets.immutableEnumSet;
import static javax.lang.model.element.Modifier.PRIVATE;
import static javax.lang.model.element.Modifier.STATIC;
import static javax.lang.model.element.NestingKind.MEMBER;
import static javax.lang.model.element.NestingKind.TOP_LEVEL;
import static net.zerobuilder.compiler.Messages.ErrorMessages.NESTING_KIND;
import static net.zerobuilder.compiler.Messages.ErrorMessages.PRIVATE_TYPE;

final class TypeValidator {

  private static final ImmutableSet<NestingKind> ALLOWED_NESTING_KINDS
      = immutableEnumSet(ImmutableSet.of(TOP_LEVEL, MEMBER));

  static void validateBuildersType(TypeElement type) throws ValidationException {
    Set<Modifier> modifiers = type.getModifiers();
    NestingKind nestingKind = type.getNestingKind();
    if (modifiers.contains(PRIVATE)) {
      throw new ValidationException(PRIVATE_TYPE, type);
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
