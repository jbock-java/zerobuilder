package net.zerobuilder.compiler.analyse;

import com.google.common.collect.ImmutableList;
import com.squareup.javapoet.ClassName;
import com.squareup.javapoet.TypeName;

public final class DtoGoal {

  static abstract class AbstractGoal {
    public final String name;
    AbstractGoal(String name) {
      this.name = name;
    }
  }

  public interface RegularGoalCases<R> {
    R method(MethodGoal goal);
    R constructor(ConstructorGoal goal);
  }

  public static abstract class RegularGoal extends AbstractGoal {

    /**
     * <p>method goal: return type</p>
     * <p>constructor goal: type of enclosing class</p>
     */
    public final TypeName goalType;

    /**
     * parameter names in original order
     */
    public final ImmutableList<String> parameterNames;

    RegularGoal(TypeName goalType, String name, ImmutableList<String> parameterNames) {
      super(name);
      this.goalType = goalType;
      this.parameterNames = parameterNames;
    }
    abstract <R> R accept(RegularGoalCases<R> cases);
  }

  public static final class ConstructorGoal extends RegularGoal {

    ConstructorGoal(TypeName goalType, String name, ImmutableList<String> parameterNames) {
      super(goalType, name, parameterNames);
    }
    @Override
    <R> R accept(RegularGoalCases<R> cases) {
      return cases.constructor(this);
    }
  }

  public static final class MethodGoal extends RegularGoal {
    public final String methodName;

    /**
     * false iff the method is static
     */
    public final boolean instance;

    MethodGoal(TypeName goalType, String name, ImmutableList<String> parameterNames, String methodName, boolean instance) {
      super(goalType, name, parameterNames);
      this.methodName = methodName;
      this.instance = instance;
    }
    @Override
    <R> R accept(RegularGoalCases<R> cases) {
      return cases.method(this);
    }
  }

  public static final class BeanGoal extends AbstractGoal {
    public final ClassName goalType;
    BeanGoal(ClassName goalType, String name) {
      super(name);
      this.goalType = goalType;
    }
  }

  private DtoGoal() {
    throw new UnsupportedOperationException("no instances");
  }
}
