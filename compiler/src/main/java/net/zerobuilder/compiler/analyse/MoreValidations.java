package net.zerobuilder.compiler.analyse;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import javax.lang.model.element.Element;
import javax.lang.model.element.Modifier;
import net.zerobuilder.compiler.analyse.DtoGoalElement.AbstractGoalElement;

import static net.zerobuilder.compiler.Messages.ErrorMessages.DUPLICATE_GOAL_NAME;
import static net.zerobuilder.compiler.Messages.ErrorMessages.PRIVATE_METHOD;
import static net.zerobuilder.compiler.analyse.DtoGoalElement.element;

final class MoreValidations {

  static void checkNameConflict(List<? extends AbstractGoalElement> goals) throws ValidationException {
    Map<String, AbstractGoalElement> m = new HashMap<>(Math.max(16, (int) (goals.size() * 1.5)));
    for (AbstractGoalElement goal : goals) {
      Element exist = element(m.get(DtoGoalElement.goalName(goal)));
      Element el = element(goal);
      if (exist != null && !exist.equals(el)) {
        throw new ValidationException(DUPLICATE_GOAL_NAME, el);
      }
      m.put(DtoGoalElement.goalName(goal), goal);
    }
  }

  static void checkAccessLevel(List<? extends AbstractGoalElement> goals) throws ValidationException {
    goals.stream().map(DtoGoalElement::element)
        .forEach(el -> {
          if (el.getModifiers().contains(Modifier.PRIVATE)) {
            throw new ValidationException(PRIVATE_METHOD, el);
          }
        });
  }

  private MoreValidations() {
    throw new UnsupportedOperationException("no instances");
  }
}
