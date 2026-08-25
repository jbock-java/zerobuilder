package net.zerobuilder.compiler.analyse;

import net.zerobuilder.compiler.analyse.DtoGoalElement.AbstractGoalElement;

import javax.lang.model.element.Element;
import javax.lang.model.element.Modifier;

import static net.zerobuilder.compiler.Messages.ErrorMessages.PRIVATE_METHOD;

final class MoreValidations {

  static void checkAccessLevel(AbstractGoalElement goal) throws ValidationException {
    Element el = DtoGoalElement.element(goal);
    if (el.getModifiers().contains(Modifier.PRIVATE)) {
      throw new ValidationException(PRIVATE_METHOD, el);
    }
  }

  private MoreValidations() {
  }
}
