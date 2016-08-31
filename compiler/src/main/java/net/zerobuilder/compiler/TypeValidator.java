package net.zerobuilder.compiler;

import javax.lang.model.element.NestingKind;
import javax.lang.model.element.TypeElement;
import java.util.EnumSet;

import static javax.lang.model.element.Modifier.PRIVATE;
import static javax.lang.model.element.Modifier.STATIC;
import static javax.lang.model.element.NestingKind.MEMBER;
import static javax.lang.model.element.NestingKind.TOP_LEVEL;
import static net.zerobuilder.compiler.Messages.ErrorMessages.NESTING_KIND;
import static net.zerobuilder.compiler.Messages.ErrorMessages.PRIVATE_TYPE;

final class TypeValidator {

  private final EnumSet<NestingKind> allowedNestingKinds = EnumSet.of(TOP_LEVEL, MEMBER);

  void validateBuildType(TypeElement buildType) throws ValidationException {
    if (buildType.getModifiers().contains(PRIVATE)) {
      throw new ValidationException(PRIVATE_TYPE, buildType);
    }
    if (!allowedNestingKinds.contains(buildType.getNestingKind())
        || (buildType.getNestingKind() == MEMBER
        && !buildType.getModifiers().contains(STATIC))) {
      throw new ValidationException(NESTING_KIND, buildType);
    }
  }

}
