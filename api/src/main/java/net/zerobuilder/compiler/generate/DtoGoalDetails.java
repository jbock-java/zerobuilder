package net.zerobuilder.compiler.generate;

import com.squareup.javapoet.ClassName;
import com.squareup.javapoet.TypeName;

import javax.lang.model.element.Modifier;
import java.util.List;
import java.util.function.Function;

public final class DtoGoalDetails {

  public enum GoalMethodType {
    STATIC_METHOD, INSTANCE_METHOD
  }

  static abstract class AbstractGoalDetails {
    final String name;
    private final Access access;

    /**
     * Returns the goal name.
     *
     * @return goal name
     */
    public final String name() {
      return name;
    }

    public final Modifier[] access(Modifier... modifiers) {
      return access.modifiers(modifiers);
    }

    abstract TypeName type();

    AbstractGoalDetails(String name, Access access) {
      this.name = name;
      this.access = access;
    }
    public abstract <R> R acceptAbstract(AbstractGoalDetailsCases<R> cases);
  }

  interface AbstractGoalDetailsCases<R> {
    R regular(AbstractRegularDetails goal);
    R bean(BeanGoalDetails goal);
  }

  interface RegularGoalDetailsCases<R> {
    R method(MethodGoalDetails goal);
    R constructor(ConstructorGoalDetails goal);
  }

  public static <R> Function<AbstractGoalDetails, R> asFunction(final AbstractGoalDetailsCases<R> cases) {
    return goal -> goal.acceptAbstract(cases);
  }

  public static <R> Function<AbstractRegularDetails, R> asFunction(final RegularGoalDetailsCases<R> cases) {
    return goal -> goal.accept(cases);
  }

  public static abstract class AbstractRegularDetails extends AbstractGoalDetails {

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
     * @param access         goal options
     */
    AbstractRegularDetails(TypeName goalType, String name, List<String> parameterNames,
                           Access access) {
      super(name, access);
      this.goalType = goalType;
      this.parameterNames = parameterNames;
    }

    abstract <R> R accept(RegularGoalDetailsCases<R> cases);

    @Override
    public final <R> R acceptAbstract(AbstractGoalDetailsCases<R> cases) {
      return cases.regular(this);
    }
  }

  public static final class ConstructorGoalDetails extends AbstractRegularDetails {

    private ConstructorGoalDetails(TypeName goalType, String name, List<String> parameterNames,
                                   Access access) {
      super(goalType, name, parameterNames, access);
    }

    public static ConstructorGoalDetails create(TypeName goalType, String name, List<String> parameterNames,
                                                Access access) {
      return new ConstructorGoalDetails(goalType, name, parameterNames, access);
    }

    @Override
    <R> R accept(RegularGoalDetailsCases<R> cases) {
      return cases.constructor(this);
    }
  }

  public static final class MethodGoalDetails extends AbstractRegularDetails {
    final String methodName;
    final GoalMethodType methodType;

    private MethodGoalDetails(TypeName goalType, String name, List<String> parameterNames, String methodName,
                              GoalMethodType methodType, Access access) {
      super(goalType, name, parameterNames, access);
      this.methodName = methodName;
      this.methodType = methodType;
    }

    public static MethodGoalDetails create(TypeName goalType,
                                           String name,
                                           List<String> parameterNames,
                                           String methodName,
                                           GoalMethodType goalMethodType,
                                           Access access) {
      return new MethodGoalDetails(goalType, name, parameterNames, methodName,
          goalMethodType, access);
    }

    @Override
    <R> R accept(RegularGoalDetailsCases<R> cases) {
      return cases.method(this);
    }
  }

  public static final class BeanGoalDetails extends AbstractGoalDetails {
    public final ClassName goalType;
    public BeanGoalDetails(ClassName goalType, String name, Access access) {
      super(name, access);
      this.goalType = goalType;
    }

    @Override
    TypeName type() {
      return goalType;
    }

    @Override
    public <R> R acceptAbstract(AbstractGoalDetailsCases<R> cases) {
      return cases.bean(this);
    }
  }

  public static final Function<AbstractGoalDetails, TypeName> goalType
      = asFunction(new AbstractGoalDetailsCases<TypeName>() {
    @Override
    public TypeName regular(AbstractRegularDetails goal) {
      return goal.goalType;
    }
    @Override
    public TypeName bean(BeanGoalDetails goal) {
      return goal.goalType;
    }
  });

  private DtoGoalDetails() {
    throw new UnsupportedOperationException("no instances");
  }
}
