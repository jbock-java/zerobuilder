package net.zerobuilder.compiler.analyse;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.Ordering;
import com.google.common.primitives.Ints;
import net.zerobuilder.Goal;
import net.zerobuilder.compiler.analyse.DtoGoalElement.AbstractGoalElement;

import javax.lang.model.element.Element;
import javax.lang.model.element.ElementKind;
import java.util.Comparator;
import java.util.HashMap;

import static com.google.common.base.Strings.isNullOrEmpty;
import static com.google.common.base.Throwables.propagate;
import static javax.lang.model.element.ElementKind.CONSTRUCTOR;
import static javax.lang.model.element.ElementKind.METHOD;
import static net.zerobuilder.compiler.Messages.ErrorMessages.GOALNAME_EECC;
import static net.zerobuilder.compiler.Messages.ErrorMessages.GOALNAME_EEMC;
import static net.zerobuilder.compiler.Messages.ErrorMessages.GOALNAME_EEMM;
import static net.zerobuilder.compiler.Messages.ErrorMessages.GOALNAME_NECC;
import static net.zerobuilder.compiler.Messages.ErrorMessages.GOALNAME_NEMC;
import static net.zerobuilder.compiler.Messages.ErrorMessages.GOALNAME_NEMM;
import static net.zerobuilder.compiler.Messages.ErrorMessages.GOALNAME_NN;
import static net.zerobuilder.compiler.analyse.DtoGoalElement.getElement;
import static net.zerobuilder.compiler.analyse.DtoGoalElement.getName;

final class GoalnameValidator {

  /**
   * to generate better error messages, in case of goal name conflict
   */
  private static final Ordering<AbstractGoalElement> GOAL_ORDER_FOR_DUPLICATE_NAME_CHECK
      = Ordering.from(new Comparator<AbstractGoalElement>() {

    private int goalWeight(AbstractGoalElement goal) throws ValidationException {
      ElementKind kind = goal.accept(getElement).getKind();
      Goal annotation = goal.goalAnnotation;
      String name = annotation.name();
      return isNullOrEmpty(name)
          ? (kind == CONSTRUCTOR ? 0 : (kind == METHOD ? 1 : 2))
          : (kind == CONSTRUCTOR ? 3 : (kind == METHOD ? 4 : 5));
    }

    @Override
    public int compare(AbstractGoalElement g0, AbstractGoalElement g1) {
      try {
        return Ints.compare(goalWeight(g0), goalWeight(g1));
      } catch (ValidationException e) {
        propagate(e);
        return 0;
      }
    }
  });


  static void checkNameConflict(ImmutableList<AbstractGoalElement> goals) throws ValidationException {
    goals = GOAL_ORDER_FOR_DUPLICATE_NAME_CHECK.immutableSortedCopy(goals);
    HashMap<String, AbstractGoalElement> byName = new HashMap<>();
    for (AbstractGoalElement goal : goals) {
      AbstractGoalElement otherGoal = byName.put(goal.accept(getName), goal);
      if (otherGoal != null) {
        Goal goalAnnotation = goal.goalAnnotation;
        Goal otherAnnotation = otherGoal.goalAnnotation;
        String thisName = goalAnnotation.name();
        String otherName = otherAnnotation.name();
        Element element = goal.accept(getElement);
        ElementKind thisKind = element.getKind();
        ElementKind otherKind = otherGoal.accept(getElement).getKind();
        if (isNullOrEmpty(thisName)) {
          if (thisKind == CONSTRUCTOR && otherKind == CONSTRUCTOR) {
            throw new ValidationException(GOALNAME_EECC, element);
          }
          if (thisKind == METHOD && otherKind == CONSTRUCTOR) {
            throw new ValidationException(GOALNAME_EEMC, element);
          }
          throw new ValidationException(GOALNAME_EEMM, element);
        } else if (isNullOrEmpty(otherName)) {
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
