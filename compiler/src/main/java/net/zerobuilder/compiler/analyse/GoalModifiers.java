package net.zerobuilder.compiler.analyse;

import net.zerobuilder.Access;
import net.zerobuilder.AccessLevel;
import net.zerobuilder.GoalName;
import net.zerobuilder.Recycle;
import net.zerobuilder.RejectNull;
import net.zerobuilder.compiler.generate.DtoContext;
import net.zerobuilder.compiler.generate.NullPolicy;

import javax.lang.model.element.ExecutableElement;

import static net.zerobuilder.compiler.analyse.DtoGoalElement.goalType;
import static net.zerobuilder.compiler.generate.ZeroUtil.downcase;
import static net.zerobuilder.compiler.generate.ZeroUtil.simpleName;

final class GoalModifiers {

  final Access access;
  final NullPolicy nullPolicy;
  final DtoContext.ContextLifecycle lifecycle;
  final String goalName;

  private GoalModifiers(Access access, NullPolicy nullPolicy, DtoContext.ContextLifecycle lifecycle, String goalName) {
    this.access = access;
    this.nullPolicy = nullPolicy;
    this.lifecycle = lifecycle;
    this.goalName = goalName;
  }

  static GoalModifiers create(ExecutableElement element) {
    Access access = element.getAnnotation(AccessLevel.class) == null ?
        Access.PUBLIC :
        element.getAnnotation(AccessLevel.class).value();
    NullPolicy nullPolicy = element.getAnnotation(RejectNull.class) == null ?
        NullPolicy.ALLOW :
        NullPolicy.REJECT;
    DtoContext.ContextLifecycle lifecycle = element.getAnnotation(Recycle.class) == null ?
        DtoContext.ContextLifecycle.NEW_INSTANCE :
        DtoContext.ContextLifecycle.REUSE_INSTANCES;
    String goalName = element.getAnnotation(GoalName.class) == null ?
        downcase(simpleName(goalType(element))) :
        element.getAnnotation(GoalName.class).value();
    return new GoalModifiers(access, nullPolicy, lifecycle, goalName);
  }
}
