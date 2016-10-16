package net.zerobuilder.compiler.generate;

import com.squareup.javapoet.ClassName;
import com.squareup.javapoet.TypeName;
import net.zerobuilder.compiler.generate.Generator.Module;

import java.util.List;
import java.util.function.Function;

public final class DtoGoal {

  public enum GoalMethodType {
    STATIC_METHOD, INSTANCE_METHOD
  }

  public static final class GoalOption {
    final Access access;
    final Module module;

    private GoalOption(Access access, Module module) {
      this.access = access;
      this.module = module;
    }

    public static GoalOption create(Access access, Module module) {
      return new GoalOption(access, module);
    }
  }

  static abstract class AbstractGoalDetails {
    final String name;
    final GoalOption goalOptions;

    /**
     * Returns the goal name.
     *
     * @return goal name
     */
    public final String name() {
      return name;
    }

    abstract TypeName type();

    AbstractGoalDetails(String name, GoalOption goalOptions) {
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
    return goal -> goal.acceptAbstract(cases);
  }

  public static <R> Function<RegularGoalDetails, R> asFunction(final RegularGoalCases<R> cases) {
    return goal -> goal.accept(cases);
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
    final List<String> parameterNames;

    @Override
    final TypeName type() {
      return goalType;
    }
    /**
     * @param goalType       goal type
     * @param name           goal name
     * @param parameterNames parameter names in original order
     * @param goalOptions    goal options
     */
    RegularGoalDetails(TypeName goalType, String name, List<String> parameterNames,
                       GoalOption goalOptions) {
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

    private ConstructorGoalDetails(TypeName goalType, String name, List<String> parameterNames,
                                   GoalOption goalOptions) {
      super(goalType, name, parameterNames, goalOptions);
    }

    public static ConstructorGoalDetails create(TypeName goalType, String name, List<String> parameterNames,
                                                GoalOption goalOptions) {
      return new ConstructorGoalDetails(goalType, name, parameterNames, goalOptions);
    }

    @Override
    <R> R accept(RegularGoalCases<R> cases) {
      return cases.constructor(this);
    }
  }

  public static final class MethodGoalDetails extends RegularGoalDetails {
    final String methodName;
    final GoalMethodType methodType;

    private MethodGoalDetails(TypeName goalType, String name, List<String> parameterNames, String methodName,
                              GoalMethodType methodType, GoalOption goalOptions) {
      super(goalType, name, parameterNames, goalOptions);
      this.methodName = methodName;
      this.methodType = methodType;
    }

    public static MethodGoalDetails create(TypeName goalType,
                                           String name,
                                           List<String> parameterNames,
                                           String methodName,
                                           GoalMethodType goalMethodType,
                                           GoalOption goalOptions) {
      return new MethodGoalDetails(goalType, name, parameterNames, methodName,
          goalMethodType, goalOptions);
    }

    @Override
    <R> R accept(RegularGoalCases<R> cases) {
      return cases.method(this);
    }
  }

  public static final class BeanGoalDetails extends AbstractGoalDetails {
    public final ClassName goalType;
    public BeanGoalDetails(ClassName goalType, String name, GoalOption goalOptions) {
      super(name, goalOptions);
      this.goalType = goalType;
    }

    @Override
    TypeName type() {
      return goalType;
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
