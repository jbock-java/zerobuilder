package net.zerobuilder.compiler;

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
import static net.zerobuilder.compiler.Messages.ErrorMessages.PRIVATE_TYPE;

final class TypeValidator {

  void validateBuildType(TypeElement buildType) throws ValidationException {
    Set<Modifier> modifiers = buildType.getModifiers();
    NestingKind nestingKind = buildType.getNestingKind();
    if (modifiers.contains(PRIVATE)) {
      throw new ValidationException(PRIVATE_TYPE, buildType);
    }
    boolean unknownType = !EnumSet.of(TOP_LEVEL, MEMBER).contains(nestingKind);
    boolean nonstaticMember = nestingKind == MEMBER && !modifiers.contains(STATIC);
    if (unknownType || nonstaticMember) {
      throw new ValidationException(NESTING_KIND, buildType);
    }
  }

}
