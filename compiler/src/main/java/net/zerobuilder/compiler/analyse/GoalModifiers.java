package net.zerobuilder.compiler.analyse;

import net.zerobuilder.AccessLevel;
import net.zerobuilder.GoalName;
import net.zerobuilder.Level;
import net.zerobuilder.compiler.generate.Access;

import javax.lang.model.element.ExecutableElement;

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
    if (accessLevel != null &&
        accessLevel.value() == Level.PACKAGE) {
      return Access.PACKAGE;
    }
    return Access.PUBLIC;
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
