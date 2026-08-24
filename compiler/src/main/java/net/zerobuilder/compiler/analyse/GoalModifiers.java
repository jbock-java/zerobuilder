package net.zerobuilder.compiler.analyse;

import javax.lang.model.element.ExecutableElement;
import javax.lang.model.element.Modifier;
import net.zerobuilder.AccessLevel;
import net.zerobuilder.GoalName;
import net.zerobuilder.Level;
import net.zerobuilder.compiler.generate.Access;

import static net.zerobuilder.compiler.analyse.DtoGoalElement.goalType;
import static net.zerobuilder.compiler.generate.ZeroUtil.downcase;
import static net.zerobuilder.compiler.generate.ZeroUtil.simpleName;

final class GoalModifiers {

  final Access access;
  final String goalName;

  private GoalModifiers(Access access, String goalName) {
    this.access = access;
    this.goalName = goalName;
  }

  private static Access getAccess(ExecutableElement element) {
    AccessLevel accessLevel = element.getAnnotation(AccessLevel.class);
    if (accessLevel != null) {
      if (accessLevel.value() == Level.PACKAGE) {
        return Access.PACKAGE;
      } else if (accessLevel.value() == Level.PUBLIC) {
        return Access.PUBLIC;
      }
    }
    if (element.getModifiers().contains(Modifier.PUBLIC)) {
      return Access.PUBLIC;
    } else {
      return Access.PACKAGE;
    }
  }

  static GoalModifiers create(ExecutableElement element) {
    Access access = getAccess(element);
    GoalName annotation = element.getAnnotation(GoalName.class);
    String goalName = annotation == null ?
        downcase(simpleName(goalType(element))) :
        annotation.value();
    return new GoalModifiers(access, goalName);
  }
}
