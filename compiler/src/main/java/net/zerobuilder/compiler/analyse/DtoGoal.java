package net.zerobuilder.compiler.analyse;

import com.google.common.base.Function;
import com.google.common.collect.ImmutableList;
import com.squareup.javapoet.ClassName;
import com.squareup.javapoet.TypeName;
import net.zerobuilder.AccessLevel;

public final class DtoGoal {

  public static final class GoalOptions {
    public final AccessLevel builderAccess;
    public final AccessLevel toBuilderAccess;

    GoalOptions(AccessLevel builderAccess, AccessLevel toBuilderAccess) {
      this.builderAccess = builderAccess;
      this.toBuilderAccess = toBuilderAccess;
    }
  }

  static abstract class AbstractGoal {
    public final String name;
    public final GoalOptions goalOptions;
    AbstractGoal(String name, GoalOptions goalOptions) {
      this.name = name;
      this.goalOptions = goalOptions;
    }
    public abstract <R> R acceptAbstract(AbstractGoalCases<R> cases);
  }

  interface AbstractGoalCases<R> {
    R regular(RegularGoal goal);
    R bean(BeanGoal goal);
  }


  interface RegularGoalCases<R> {
    R method(MethodGoal goal);
    R constructor(ConstructorGoal goal);
  }

  static <R> Function<AbstractGoal, R> asFunction(final AbstractGoalCases<R> cases) {
    return new Function<AbstractGoal, R>() {
      @Override
      public R apply(AbstractGoal goal) {
        return goal.acceptAbstract(cases);
      }
    };
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

    RegularGoal(TypeName goalType, String name, ImmutableList<String> parameterNames,
                GoalOptions goalOptions) {
      super(name, goalOptions);
      this.goalType = goalType;
      this.parameterNames = parameterNames;
    }
    abstract <R> R accept(RegularGoalCases<R> cases);

    @Override
    public final <R> R acceptAbstract(AbstractGoalCases<R> cases) {
      return cases.regular(this);
    }
  }

  public static final class ConstructorGoal extends RegularGoal {

    ConstructorGoal(TypeName goalType, String name, ImmutableList<String> parameterNames,
                    GoalOptions goalOptions) {
      super(goalType, name, parameterNames, goalOptions);
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

    MethodGoal(TypeName goalType, String name, ImmutableList<String> parameterNames, String methodName,
               boolean instance, GoalOptions goalOptions) {
      super(goalType, name, parameterNames, goalOptions);
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
    BeanGoal(ClassName goalType, String name, GoalOptions goalOptions) {
      super(name, goalOptions);
      this.goalType = goalType;
    }

    @Override
    public <R> R acceptAbstract(AbstractGoalCases<R> cases) {
      return cases.bean(this);
    }
  }

  static final Function<AbstractGoal, TypeName> goalType
      = asFunction(new AbstractGoalCases<TypeName>() {
    @Override
    public TypeName regular(RegularGoal goal) {
      return goal.goalType;
    }
    @Override
    public TypeName bean(BeanGoal goal) {
      return goal.goalType;
    }
  });

  private DtoGoal() {
    throw new UnsupportedOperationException("no instances");
  }
}
