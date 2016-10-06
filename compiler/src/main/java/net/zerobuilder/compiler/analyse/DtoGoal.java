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
    public final boolean toBuilder;
    public final boolean builder;


    GoalOptions(AccessLevel builderAccess, AccessLevel toBuilderAccess, boolean toBuilder, boolean builder) {
      this.builderAccess = builderAccess;
      this.toBuilderAccess = toBuilderAccess;
      this.toBuilder = toBuilder;
      this.builder = builder;
    }
  }

  public static abstract class AbstractGoal {
    public final String name;
    public final GoalOptions goalOptions;
    AbstractGoal(String name, GoalOptions goalOptions) {
      this.name = name;
      this.goalOptions = goalOptions;
    }
    public abstract <R> R acceptAbstract(AbstractGoalCases<R> cases);
  }

  public interface AbstractGoalCases<R> {
    R regular(AbstractRegularGoal goal);
    R bean(BeanGoalDetails goal);
  }

  public interface RegularGoalCases<R> {
    R method(MethodGoalDetails goal);
    R constructor(ConstructorGoalDetails goal);
  }

  public static <R> Function<AbstractGoal, R> asFunction(final AbstractGoalCases<R> cases) {
    return new Function<AbstractGoal, R>() {
      @Override
      public R apply(AbstractGoal goal) {
        return goal.acceptAbstract(cases);
      }
    };
  }

  public static <R> Function<AbstractRegularGoal, R> asFunction(final RegularGoalCases<R> cases) {
    return new Function<AbstractRegularGoal, R>() {
      @Override
      public R apply(AbstractRegularGoal goal) {
        return goal.accept(cases);
      }
    };
  }

  public static abstract class AbstractRegularGoal extends AbstractGoal {

    /**
     * <p>method goal: return type</p>
     * <p>constructor goal: type of enclosing class</p>
     */
    public final TypeName goalType;

    /**
     * parameter names in original order
     */
    public final ImmutableList<String> parameterNames;

    AbstractRegularGoal(TypeName goalType, String name, ImmutableList<String> parameterNames,
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

  public static final class ConstructorGoalDetails extends AbstractRegularGoal {

    ConstructorGoalDetails(TypeName goalType, String name, ImmutableList<String> parameterNames,
                           GoalOptions goalOptions) {
      super(goalType, name, parameterNames, goalOptions);
    }
    @Override
    <R> R accept(RegularGoalCases<R> cases) {
      return cases.constructor(this);
    }
  }

  public static final class MethodGoalDetails extends AbstractRegularGoal {
    public final String methodName;

    /**
     * false iff the method is static
     */
    public final boolean instance;

    MethodGoalDetails(TypeName goalType, String name, ImmutableList<String> parameterNames, String methodName,
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

  public static final class BeanGoalDetails extends AbstractGoal {
    public final ClassName goalType;
    BeanGoalDetails(ClassName goalType, String name, GoalOptions goalOptions) {
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
    public TypeName regular(AbstractRegularGoal goal) {
      return goal.goalType;
    }
    @Override
    public TypeName bean(BeanGoalDetails goal) {
      return goal.goalType;
    }
  });

  private DtoGoal() {
    throw new UnsupportedOperationException("no instances");
  }
}
