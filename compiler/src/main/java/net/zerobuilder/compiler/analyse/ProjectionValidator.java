package net.zerobuilder.compiler.analyse;

import com.google.common.base.Function;
import com.google.common.base.Optional;
import com.google.common.collect.ImmutableList;
import com.squareup.javapoet.ClassName;
import com.squareup.javapoet.TypeName;
import net.zerobuilder.Step;
import net.zerobuilder.compiler.analyse.Analyser.AbstractGoalElement.GoalElementCases;
import net.zerobuilder.compiler.analyse.Analyser.BeanGoal;
import net.zerobuilder.compiler.analyse.ProjectionValidator.ValidParameter.AccessorPair;
import net.zerobuilder.compiler.analyse.ProjectionValidator.ValidParameter.RegularParameter;

import javax.lang.model.element.Element;
import javax.lang.model.element.ExecutableElement;
import javax.lang.model.element.VariableElement;
import javax.lang.model.type.TypeMirror;
import java.util.ArrayList;
import java.util.List;

import static com.google.common.base.Optional.fromNullable;
import static java.util.Collections.nCopies;
import static javax.tools.Diagnostic.Kind.ERROR;
import static net.zerobuilder.compiler.Messages.ErrorMessages.DUPLICATE_STEP_POSITION;
import static net.zerobuilder.compiler.Messages.ErrorMessages.STEP_POSITION_TOO_LARGE;
import static net.zerobuilder.compiler.Utilities.downcase;
import static net.zerobuilder.compiler.analyse.Analyser.goalElementCases;
import static net.zerobuilder.compiler.analyse.ProjectionValidatorB.validateBean;
import static net.zerobuilder.compiler.analyse.ProjectionValidatorV.validateValue;
import static net.zerobuilder.compiler.analyse.ProjectionValidatorV.validateValueSkipProjections;

public final class ProjectionValidator {

  static final GoalElementCases<ValidationResult> validate = goalElementCases(validateValue, validateBean);
  static final GoalElementCases<ValidationResult> skip = goalElementCases(validateValueSkipProjections, validateBean);

  static <E extends TmpValidParameter> ImmutableList<E> shuffledParameters(ImmutableList<E> parameters)
      throws ValidationException {
    List<E> builder = new ArrayList(nCopies(parameters.size(), null));
    ImmutableList.Builder<E> noAnnotation = ImmutableList.builder();
    for (E parameter : parameters) {
      Optional<Step> step = parameter.annotation;
      if (step.isPresent() && step.get().value() >= 0) {
        int value = step.get().value();
        if (value >= parameters.size()) {
          throw new ValidationException(ERROR,
              STEP_POSITION_TOO_LARGE, parameter.element);
        }
        if (builder.get(value) != null) {
          throw new ValidationException(ERROR,
              DUPLICATE_STEP_POSITION, parameter.element);
        }
        builder.set(value, parameter);
      } else {
        noAnnotation.add(parameter);
      }
    }
    int pos = 0;
    for (E parameter : noAnnotation.build()) {
      while (builder.get(pos) != null) {
        pos++;
      }
      builder.set(pos++, parameter);
    }
    return ImmutableList.copyOf(builder);
  }

  abstract static class TmpValidParameter {

    final Element element;
    final Optional<Step> annotation;

    final static class TmpRegularParameter extends TmpValidParameter {
      private final RegularParameter parameter;
      private TmpRegularParameter(Element element, Optional<Step> annotation, RegularParameter parameter) {
        super(element, annotation);
        this.parameter = parameter;
      }
      static final Function<TmpRegularParameter, RegularParameter> toValidParameter = new Function<TmpRegularParameter, RegularParameter>() {
        @Override
        public RegularParameter apply(TmpRegularParameter parameter) {
          return parameter.parameter;
        }
      };
      static TmpRegularParameter create(VariableElement element, Optional<String> projectionMethodName) {
        Optional<Step> annotation = fromNullable(element.getAnnotation(Step.class));
        boolean nonNull = annotation.isPresent() && annotation.get().nonNull();
        RegularParameter parameter = new RegularParameter(element.getSimpleName().toString(),
            TypeName.get(element.asType()), projectionMethodName, nonNull);
        return new TmpRegularParameter(element, annotation, parameter);
      }
    }

    static final class TmpAccessorPair extends TmpValidParameter {
      final AccessorPair accessorPair;
      private TmpAccessorPair(Element element, Optional<Step> annotation, AccessorPair accessorPair) {
        super(element, annotation);
        this.accessorPair = accessorPair;
      }
      static final Function<TmpAccessorPair, AccessorPair> toValidParameter = new Function<TmpAccessorPair, AccessorPair>() {
        @Override
        public AccessorPair apply(TmpAccessorPair parameter) {
          return parameter.accessorPair;
        }
      };
      static TmpAccessorPair create(ExecutableElement getter, CollectionType collectionType) {
        Optional<Step> annotation = fromNullable(getter.getAnnotation(Step.class));
        boolean nonNull = annotation.isPresent() && annotation.get().nonNull();
        AccessorPair accessorPair = new AccessorPair(TypeName.get(getter.getReturnType()),
            getter.getSimpleName().toString(), collectionType, nonNull);
        return new TmpAccessorPair(getter, annotation, accessorPair);
      }
    }

    private TmpValidParameter(Element element, Optional<Step> annotation) {
      this.element = element;
      this.annotation = annotation;
    }

  }

  public abstract static class ValidParameter {

    public final String name;
    public final TypeName type;
    public final boolean nonNull;

    public static final class RegularParameter extends ValidParameter {

      /**
       * absent iff {@code toBuilder = false} or direct field access
       */
      public final Optional<String> projectionMethodName;
      RegularParameter(String name, TypeName type, Optional<String> projectionMethodName, boolean nonNull) {
        super(name, type, nonNull);
        this.projectionMethodName = projectionMethodName;
      }
    }

    public static final class AccessorPair extends ValidParameter {
      public final String projectionMethodName;

      public final CollectionType collectionType;

      AccessorPair(TypeName type, String projectionMethodName, CollectionType collectionType, boolean nonNull) {
        super(name(projectionMethodName), type, nonNull);
        this.projectionMethodName = projectionMethodName;
        this.collectionType = collectionType;
      }

      private static String name(String projectionMethodName) {
        return downcase(projectionMethodName.substring(projectionMethodName.startsWith("is") ? 2 : 3));
      }
    }

    ValidParameter(String name, TypeName type, boolean nonNull) {
      this.name = name;
      this.type = type;
      this.nonNull = nonNull;
    }
  }

  public static abstract class ValidationResult {
    public static abstract class ValidationResultCases<R> {
      abstract R executableGoal(Analyser.ExecutableGoal goal, ImmutableList<RegularParameter> parameters);
      abstract R beanGoal(BeanGoal beanGoal, ImmutableList<AccessorPair> accessorPairs);
    }
    abstract <R> R accept(ValidationResultCases<R> cases);
    static final class RegularValidationResult extends ValidationResult {
      private final Analyser.ExecutableGoal goal;
      private final ImmutableList<RegularParameter> parameters;
      RegularValidationResult(Analyser.ExecutableGoal goal, ImmutableList<RegularParameter> parameters) {
        this.goal = goal;
        this.parameters = parameters;
      }
      @Override
      <R> R accept(ValidationResultCases<R> cases) {
        return cases.executableGoal(goal, parameters);
      }
    }

    static final class BeanValidationResult extends ValidationResult {
      private final BeanGoal goal;
      private final ImmutableList<AccessorPair> accessorPairs;
      BeanValidationResult(BeanGoal goal, ImmutableList<AccessorPair> accessorPairs) {
        this.goal = goal;
        this.accessorPairs = accessorPairs;
      }
      @Override
      <R> R accept(ValidationResultCases<R> cases) {
        return cases.beanGoal(goal, accessorPairs);
      }
    }
  }

  public static final class CollectionType {

    /**
     * Present iff this parameter is a setterless collection.
     * For example, if the parameter is of type {@code List<String>}, this would be {@code String}.
     */
    public final Optional<? extends TypeName> type;

    public final boolean allowShortcut;

    private CollectionType(Optional<? extends TypeName> type, boolean allowShortcut) {
      this.type = type;
      this.allowShortcut = allowShortcut;
    }
    static final CollectionType absent = new CollectionType(Optional.<TypeName>absent(), false);
    static CollectionType of(TypeMirror type, boolean allowShortcut) {
      return new CollectionType(Optional.of(TypeName.get(type)), allowShortcut);
    }
    static CollectionType of(Class clazz, boolean allowShortcut) {
      return new CollectionType(Optional.of(ClassName.get(clazz)), allowShortcut);
    }
  }

  private ProjectionValidator() {
    throw new UnsupportedOperationException("no instances");
  }
}
