package net.zerobuilder.compiler.analyse;

import com.google.common.base.Optional;
import com.google.common.collect.ImmutableList;
import com.squareup.javapoet.ClassName;
import com.squareup.javapoet.TypeName;
import net.zerobuilder.compiler.analyse.DtoPackage.GoalTypes.BeanGoal;
import net.zerobuilder.compiler.analyse.DtoPackage.GoalTypes.ExecutableGoal;

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

  public static abstract class ValidGoal {
    public static abstract class ValidationResultCases<R> {
      abstract R executableGoal(ExecutableGoal goal, ImmutableList<ValidRegularParameter> parameters);
      abstract R beanGoal(BeanGoal beanGoal, ImmutableList<ValidBeanParameter> validBeanParameters);
    }
    abstract <R> R accept(ValidationResultCases<R> cases);
  }

  static final class ValidRegularGoal extends ValidGoal {
    private final ExecutableGoal goal;
    private final ImmutableList<ValidRegularParameter> parameters;
    ValidRegularGoal(ExecutableGoal goal, ImmutableList<ValidRegularParameter> parameters) {
      this.goal = goal;
      this.parameters = parameters;
    }
    @Override
    <R> R accept(ValidationResultCases<R> cases) {
      return cases.executableGoal(goal, parameters);
    }
  }

  static final class ValidBeanGoal extends ValidGoal {
    private final BeanGoal goal;
    private final ImmutableList<ValidBeanParameter> validBeanParameters;
    ValidBeanGoal(BeanGoal goal, ImmutableList<ValidBeanParameter> validBeanParameters) {
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

  private DtoShared() {
    throw new UnsupportedOperationException("no instances");
  }
}
