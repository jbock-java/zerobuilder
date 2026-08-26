package net.zerobuilder.compiler.analyse;

import javax.lang.model.element.Element;
import javax.lang.model.element.Modifier;

import static net.zerobuilder.compiler.Messages.ErrorMessages.PRIVATE_METHOD;

final class MoreValidations {

  static void checkAccessLevel(GoalElement goal) throws ValidationException {
    Element el = goal.executableElement();
    if (el.getModifiers().contains(Modifier.PRIVATE)) {
      throw new ValidationException(PRIVATE_METHOD, el);
    }
  }

  private MoreValidations() {
  }
}
