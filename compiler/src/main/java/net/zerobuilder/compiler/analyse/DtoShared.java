package net.zerobuilder.compiler.analyse;

import com.google.common.base.Optional;
import com.google.common.collect.ImmutableList;
import com.squareup.javapoet.ClassName;
import com.squareup.javapoet.TypeName;
import net.zerobuilder.compiler.analyse.DtoPackage.GoalTypes.BeanGoalElement;
import net.zerobuilder.compiler.analyse.DtoPackage.GoalTypes.RegularGoalElement;
import net.zerobuilder.compiler.analyse.GoalContextFactory.GoalKind;
import net.zerobuilder.compiler.generate.DtoBuilders.BuildersContext;
import net.zerobuilder.compiler.generate.DtoGoal.AbstractGoalContext;

import javax.lang.model.type.TypeMirror;

import static net.zerobuilder.compiler.Utilities.downcase;

public final class DtoShared {

  public abstract static class ValidParameter {

    public final String name;
    public final TypeName type;
    public final boolean nonNull;

    ValidParameter(String name, TypeName type, boolean nonNull) {
      this.name = name;
      this.type = type;
      this.nonNull = nonNull;
    }
  }

  static abstract class ValidGoal {
    static abstract class ValidationResultCases<R> {
      abstract R executableGoal(RegularGoalElement goal, ImmutableList<ValidRegularParameter> parameters);
      abstract R beanGoal(BeanGoalElement beanGoal, ImmutableList<ValidBeanParameter> validBeanParameters);
    }
    abstract <R> R accept(ValidationResultCases<R> cases);
  }

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

  static final class ValidRegularGoal extends ValidGoal {
    private final RegularGoalElement goal;
    private final ImmutableList<ValidRegularParameter> parameters;
    ValidRegularGoal(RegularGoalElement goal, ImmutableList<ValidRegularParameter> parameters) {
      this.goal = goal;
      this.parameters = parameters;
    }
    @Override
    <R> R accept(ValidationResultCases<R> cases) {
      return cases.executableGoal(goal, parameters);
    }
  }

  static final class ValidBeanGoal extends ValidGoal {
    private final BeanGoalElement goal;
    private final ImmutableList<ValidBeanParameter> validBeanParameters;
    ValidBeanGoal(BeanGoalElement goal, ImmutableList<ValidBeanParameter> validBeanParameters) {
      this.goal = goal;
      this.validBeanParameters = validBeanParameters;
    }
    @Override
    <R> R accept(ValidationResultCases<R> cases) {
      return cases.beanGoal(goal, validBeanParameters);
    }
  }

  public static final class ValidRegularParameter extends ValidParameter {

    /**
     * absent iff {@code toBuilder = false} or direct field access
     */
    public final Optional<String> projectionMethodName;
    ValidRegularParameter(String name, TypeName type, Optional<String> projectionMethodName, boolean nonNull) {
      super(name, type, nonNull);
      this.projectionMethodName = projectionMethodName;
    }
  }

  public static final class ValidBeanParameter extends ValidParameter {

    /**
     * Name of the getter method (could start with "is")
     */
    public final String projectionMethodName;

    /**
     * Contains details about generics if this is a collection-returning lone getter,
     * otherwise {@link CollectionType#ABSENT}
     */
    public final CollectionType collectionType;

    ValidBeanParameter(TypeName type, String projectionMethodName, CollectionType collectionType, boolean nonNull) {
      super(name(projectionMethodName), type, nonNull);
      this.projectionMethodName = projectionMethodName;
      this.collectionType = collectionType;
    }

    private static String name(String projectionMethodName) {
      return downcase(projectionMethodName.substring(projectionMethodName.startsWith("is") ? 2 : 3));
    }

    public static final class CollectionType {

      /**
       * Present iff this parameter is a setterless collection.
       * For example, if the parameter is of type {@code List<String>}, this would be {@code String}.
       */
      private final Optional<? extends TypeName> type;
      public final boolean allowShortcut;

      public boolean isPresent() {
        return type.isPresent();
      }
      public TypeName get() {
        return type.get();
      }

      private CollectionType(Optional<? extends TypeName> type, boolean allowShortcut) {
        this.type = type;
        this.allowShortcut = allowShortcut;
      }
      static final CollectionType ABSENT = new CollectionType(Optional.<TypeName>absent(), false);
      static CollectionType of(TypeMirror type, boolean allowShortcut) {
        return new CollectionType(Optional.of(TypeName.get(type)), allowShortcut);
      }
      static CollectionType of(Class clazz, boolean allowShortcut) {
        return new CollectionType(Optional.of(ClassName.get(clazz)), allowShortcut);
      }
    }
  }

  public static final class AnalysisResult {
    public final BuildersContext builders;
    public final ImmutableList<AbstractGoalContext> goals;

    AnalysisResult(BuildersContext builders, ImmutableList<AbstractGoalContext> goals) {
      this.builders = builders;
      this.goals = goals;
    }
  }

  private DtoShared() {
    throw new UnsupportedOperationException("no instances");
  }
}
