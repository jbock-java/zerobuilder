package net.zerobuilder.compiler.analyse;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.Ordering;
import com.google.common.primitives.Ints;
import net.zerobuilder.Goal;

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

final class GoalnameValidator {

  /**
   * to generate better error messages, in case of goal name conflict
   */
  private static final Ordering<Analyser.GoalElement> GOAL_ORDER_FOR_DUPLICATE_NAME_CHECK
      = Ordering.from(new Comparator<Analyser.GoalElement>() {

    private int goalWeight(Analyser.GoalElement goal) throws ValidationException {
      ElementKind kind = goal.accept(getElement).getKind();
      Goal annotation = goal.goalAnnotation;
      String name = annotation.name();
      return isNullOrEmpty(name)
          ? (kind == CONSTRUCTOR ? 0 : (kind == METHOD ? 1 : 2))
          : (kind == CONSTRUCTOR ? 3 : (kind == METHOD ? 4 : 5));
    }

    @Override
    public int compare(Analyser.GoalElement g0, Analyser.GoalElement g1) {
      try {
        return Ints.compare(goalWeight(g0), goalWeight(g1));
      } catch (ValidationException e) {
        propagate(e);
        return 0;
      }
    }
  });


  static void checkNameConflict(ImmutableList<Analyser.GoalElement> goals) throws ValidationException {
    goals = GOAL_ORDER_FOR_DUPLICATE_NAME_CHECK.immutableSortedCopy(goals);
    HashMap<String, Analyser.GoalElement> goalNames = new HashMap<>();
    for (Analyser.GoalElement goal : goals) {
      Analyser.GoalElement otherGoal = goalNames.put(goal.name, goal);
      if (otherGoal != null) {
        Goal goalAnnotation = goal.goalAnnotation;
        Goal otherAnnotation = otherGoal.goalAnnotation;
        String thisName = goalAnnotation.name();
        String otherName = otherAnnotation.name();
        ElementKind thisKind = goal.accept(getElement).getKind();
        ElementKind otherKind = otherGoal.accept(getElement).getKind();
        if (isNullOrEmpty(thisName)) {
          if (thisKind == CONSTRUCTOR && otherKind == CONSTRUCTOR) {
            throw new ValidationException(GOALNAME_EECC, goal.accept(getElement));
          }
          if (thisKind == METHOD && otherKind == CONSTRUCTOR) {
            throw new ValidationException(GOALNAME_EEMC, goal.accept(getElement));
          }
          throw new ValidationException(GOALNAME_EEMM, goal.accept(getElement));
        } else if (isNullOrEmpty(otherName)) {
          if (thisKind == CONSTRUCTOR && otherKind == CONSTRUCTOR) {
            throw new ValidationException(GOALNAME_NECC, goal.accept(getElement));
          }
          if (thisKind == METHOD && otherKind == CONSTRUCTOR) {
            throw new ValidationException(GOALNAME_NEMC, goal.accept(getElement));
          }
          throw new ValidationException(GOALNAME_NEMM, goal.accept(getElement));
        }
        throw new ValidationException(GOALNAME_NN, goal.accept(getElement));
      }
    }
  }

  private static final Analyser.AbstractGoalElement.GoalElementCases<Element> getElement = new Analyser.AbstractGoalElement.GoalElementCases<Element>() {
    @Override
    public Element executableGoal(Analyser.ExecutableGoal executableGoal) {
      return executableGoal.executableElement;
    }
    @Override
    public Element beanGoal(Analyser.BeanGoal beanGoal) {
      return beanGoal.beanTypeElement;
    }
  };

  private GoalnameValidator() {
    throw new UnsupportedOperationException("no instances");
  }
}
