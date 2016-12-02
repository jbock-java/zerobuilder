package net.zerobuilder.compiler.analyse;

import net.zerobuilder.AccessLevel;
import net.zerobuilder.NullPolicy;
import net.zerobuilder.Recycle;
import net.zerobuilder.RejectNull;
import net.zerobuilder.compiler.generate.Access;
import net.zerobuilder.compiler.generate.DtoContext;

import javax.lang.model.element.ExecutableElement;

final class GoalModifiers {

  final Access access;
  final NullPolicy nullPolicy;
  final DtoContext.ContextLifecycle lifecycle;

  private GoalModifiers(Access access, NullPolicy nullPolicy, DtoContext.ContextLifecycle lifecycle) {
    this.access = access;
    this.nullPolicy = nullPolicy;
    this.lifecycle = lifecycle;
  }

  static GoalModifiers create(ExecutableElement element) {
    AccessLevel accessLevel = element.getAnnotation(AccessLevel.class);
    Access access = accessLevel == null ? Access.PUBLIC : accessLevel.value();
    NullPolicy nullPolicy = element.getAnnotation(RejectNull.class) == null ?
        NullPolicy.ALLOW :
        NullPolicy.REJECT;
    DtoContext.ContextLifecycle lifecycle = element.getAnnotation(Recycle.class) == null ?
        DtoContext.ContextLifecycle.NEW_INSTANCE :
        DtoContext.ContextLifecycle.REUSE_INSTANCES;
    return new GoalModifiers(access, nullPolicy, lifecycle);
  }
}
