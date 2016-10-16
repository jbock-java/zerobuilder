package net.zerobuilder.compiler.analyse;

import net.zerobuilder.Goal;
import net.zerobuilder.compiler.analyse.Analyser.NameElement;

import javax.lang.model.element.Element;
import javax.lang.model.element.ElementKind;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;

import static javax.lang.model.element.ElementKind.CONSTRUCTOR;
import static javax.lang.model.element.ElementKind.METHOD;
import static net.zerobuilder.compiler.Messages.ErrorMessages.GOALNAME_EECC;
import static net.zerobuilder.compiler.Messages.ErrorMessages.GOALNAME_EEMC;
import static net.zerobuilder.compiler.Messages.ErrorMessages.GOALNAME_EEMM;
import static net.zerobuilder.compiler.Messages.ErrorMessages.GOALNAME_NECC;
import static net.zerobuilder.compiler.Messages.ErrorMessages.GOALNAME_NEMC;
import static net.zerobuilder.compiler.Messages.ErrorMessages.GOALNAME_NEMM;
import static net.zerobuilder.compiler.Messages.ErrorMessages.GOALNAME_NN;
import static net.zerobuilder.compiler.analyse.Utilities.sortedCopy;

final class GoalnameValidator {

  private static int goalWeight(NameElement goal) throws ValidationException {
    ElementKind kind = goal.element.getKind();
    Goal annotation = goal.goalAnnotation;
    String name = annotation.name();
    return name.isEmpty()
        ? (kind == CONSTRUCTOR ? 0 : (kind == METHOD ? 1 : 2))
        : (kind == CONSTRUCTOR ? 3 : (kind == METHOD ? 4 : 5));
  }

  /**
   * to generate better error messages, in case of goal name conflict
   */
  static final Comparator<NameElement> INTERMEDIATE_GOAL_ORDER = (g0, g1) -> Integer.compare(goalWeight(g0), goalWeight(g1));

  static void checkNameConflict(List<NameElement> goals) throws ValidationException {
    goals = sortedCopy(goals, INTERMEDIATE_GOAL_ORDER);
    HashMap<String, NameElement> byName = new HashMap<>();
    for (NameElement goal : goals) {
      NameElement otherGoal = byName.put(goal.name, goal);
      if (otherGoal != null) {
        Goal goalAnnotation = goal.goalAnnotation;
        Goal otherAnnotation = otherGoal.goalAnnotation;
        String thisName = goalAnnotation.name();
        String otherName = otherAnnotation.name();
        Element element = goal.element;
        ElementKind thisKind = element.getKind();
        ElementKind otherKind = otherGoal.element.getKind();
        if (thisName.isEmpty()) {
          if (thisKind == CONSTRUCTOR && otherKind == CONSTRUCTOR) {
            throw new ValidationException(GOALNAME_EECC, element);
          }
          if (thisKind == METHOD && otherKind == CONSTRUCTOR) {
            throw new ValidationException(GOALNAME_EEMC, element);
          }
          throw new ValidationException(GOALNAME_EEMM, element);
        } else if (otherName.isEmpty()) {
          if (thisKind == CONSTRUCTOR && otherKind == CONSTRUCTOR) {
            throw new ValidationException(GOALNAME_NECC, element);
          }
          if (thisKind == METHOD && otherKind == CONSTRUCTOR) {
            throw new ValidationException(GOALNAME_NEMC, element);
          }
          throw new ValidationException(GOALNAME_NEMM, element);
        }
        throw new ValidationException(GOALNAME_NN, element);
      }
    }
  }

  private GoalnameValidator() {
    throw new UnsupportedOperationException("no instances");
  }
}
