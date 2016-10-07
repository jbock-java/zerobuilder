package net.zerobuilder.compiler.generate;

import com.google.common.base.Function;
import com.google.common.collect.ImmutableList;
import com.squareup.javapoet.ClassName;
import com.squareup.javapoet.TypeName;
import net.zerobuilder.AccessLevel;

import java.util.List;

import static net.zerobuilder.AccessLevel.PUBLIC;
import static net.zerobuilder.AccessLevel.UNSPECIFIED;

public final class DtoGoal {

  public static final class GoalOptions {
    final AccessLevel builderAccess;
    final AccessLevel toBuilderAccess;
    final boolean toBuilder;
    final boolean builder;

    private GoalOptions(AccessLevel builderAccess, AccessLevel toBuilderAccess, boolean toBuilder, boolean builder) {
      this.builderAccess = builderAccess;
      this.toBuilderAccess = toBuilderAccess;
      this.toBuilder = toBuilder;
      this.builder = builder;
    }

    public static Builder builder() {
      return new Builder();
    }

    public static final class Builder {
      private AccessLevel builderAccess = PUBLIC;
      private AccessLevel toBuilderAccess = PUBLIC;
      private boolean toBuilder = false;
      private boolean builder = true;
      private Builder() {
      }
      public Builder builderAccess(AccessLevel builderAccess) {
        this.builderAccess = builderAccess;
        return this;
      }
      public Builder toBuilderAccess(AccessLevel toBuilderAccess) {
        this.toBuilderAccess = toBuilderAccess;
        return this;
      }
      public Builder toBuilder(boolean toBuilder) {
        this.toBuilder = toBuilder;
        return this;
      }
      public Builder builder(boolean builder) {
        this.builder = builder;
        return this;
      }
      public GoalOptions build() {
        return new GoalOptions(builderAccess, toBuilderAccess, toBuilder, builder);
      }
    }
  }

  static abstract class AbstractGoalDetails {
    final String name;
    final GoalOptions goalOptions;

    /**
     * Returns the goal name.
     * @return goal name
     */
    public final String name() {
      return name;
    }

    AbstractGoalDetails(String name, GoalOptions goalOptions) {
      this.name = name;
      this.goalOptions = goalOptions;
    }
    public abstract <R> R acceptAbstract(AbstractGoalCases<R> cases);
  }

  interface AbstractGoalCases<R> {
    R regular(RegularGoalDetails goal);
    R bean(BeanGoalDetails goal);
  }

  interface RegularGoalCases<R> {
    R method(MethodGoalDetails goal);
    R constructor(ConstructorGoalDetails goal);
  }

  public static <R> Function<AbstractGoalDetails, R> asFunction(final AbstractGoalCases<R> cases) {
    return new Function<AbstractGoalDetails, R>() {
      @Override
      public R apply(AbstractGoalDetails goal) {
        return goal.acceptAbstract(cases);
      }
    };
  }

  public static <R> Function<RegularGoalDetails, R> asFunction(final RegularGoalCases<R> cases) {
    return new Function<RegularGoalDetails, R>() {
      @Override
      public R apply(RegularGoalDetails goal) {
        return goal.accept(cases);
      }
    };
  }

  public static abstract class RegularGoalDetails extends AbstractGoalDetails {

    /**
     * <p>method goal: return type</p>
     * <p>constructor goal: type of enclosing class</p>
     */
    final TypeName goalType;

    /**
     * parameter names in original order
     */
    final ImmutableList<String> parameterNames;

    RegularGoalDetails(TypeName goalType, String name, ImmutableList<String> parameterNames,
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

  public static final class ConstructorGoalDetails extends RegularGoalDetails {

    private ConstructorGoalDetails(TypeName goalType, String name, ImmutableList<String> parameterNames,
                                   GoalOptions goalOptions) {
      super(goalType, name, parameterNames, goalOptions);
    }

    public static ConstructorGoalDetails create(TypeName goalType, String name, List<String> parameterNames,
                                                GoalOptions goalOptions) {
      return new ConstructorGoalDetails(goalType, name, ImmutableList.copyOf(parameterNames), goalOptions);
    }

    @Override
    <R> R accept(RegularGoalCases<R> cases) {
      return cases.constructor(this);
    }
  }

  public static final class MethodGoalDetails extends RegularGoalDetails {
    final String methodName;

    /**
     * {@code false} iff the method is {@code static}
     */
    final boolean instance;

    private MethodGoalDetails(TypeName goalType, String name, ImmutableList<String> parameterNames, String methodName,
                              boolean instance, GoalOptions goalOptions) {
      super(goalType, name, parameterNames, goalOptions);
      this.methodName = methodName;
      this.instance = instance;
    }

    public static MethodGoalDetails create(TypeName goalType, String name, List<String> parameterNames, String methodName,
                                           boolean instance, GoalOptions goalOptions) {
      return new MethodGoalDetails(goalType, name, ImmutableList.copyOf(parameterNames), methodName, instance, goalOptions);
    }

    @Override
    <R> R accept(RegularGoalCases<R> cases) {
      return cases.method(this);
    }
  }

  public static final class BeanGoalDetails extends AbstractGoalDetails {
    public final ClassName goalType;
    public BeanGoalDetails(ClassName goalType, String name, GoalOptions goalOptions) {
      super(name, goalOptions);
      this.goalType = goalType;
    }

    @Override
    public <R> R acceptAbstract(AbstractGoalCases<R> cases) {
      return cases.bean(this);
    }
  }

  public static final Function<AbstractGoalDetails, TypeName> goalType
      = asFunction(new AbstractGoalCases<TypeName>() {
    @Override
    public TypeName regular(RegularGoalDetails goal) {
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
