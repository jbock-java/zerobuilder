package net.zerobuilder.compiler.analyse;

import com.google.common.collect.ImmutableList;
import com.squareup.javapoet.ClassName;
import com.squareup.javapoet.TypeName;
import net.zerobuilder.compiler.analyse.GoalContextFactory.GoalKind;

public final class DtoGoal {

  static abstract class AbstractGoal {
    public final String name;
    AbstractGoal(String name) {
      this.name = name;
    }
    public abstract <R> R accept(AbstractGoalCases<R> cases);
  }

  interface AbstractGoalCases<R> {
    R regularGoal(RegularGoal goal);
    R beanGoal(BeanGoal goal);
  }

  public static final class RegularGoal extends AbstractGoal {

    /**
     * <p>method goal: return type</p>
     * <p>constructor goal: type of enclosing class</p>
     */
    public final TypeName goalType;
    public final GoalKind kind;

    /**
     * parameter names in original order
     */
    public final ImmutableList<String> parameterNames;

    /**
     * empty string for constructor goals
     */
    public final String methodName;

    RegularGoal(TypeName goalType, String name, GoalKind kind, ImmutableList<String> parameterNames, String methodName) {
      super(name);
      this.goalType = goalType;
      this.kind = kind;
      this.parameterNames = parameterNames;
      this.methodName = methodName;
    }

    @Override
    public <R> R accept(AbstractGoalCases<R> cases) {
      return cases.regularGoal(this);
    }
  }

  public static final class BeanGoal extends AbstractGoal {
    public final ClassName goalType;
    BeanGoal(ClassName goalType, String name) {
      super(name);
      this.goalType = goalType;
    }
    @Override
    public <R> R accept(AbstractGoalCases<R> cases) {
      return cases.beanGoal(this);
    }
  }

  private DtoGoal() {
    throw new UnsupportedOperationException("no instances");
  }
}
