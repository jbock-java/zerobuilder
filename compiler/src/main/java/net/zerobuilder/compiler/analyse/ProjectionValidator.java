package net.zerobuilder.compiler.analyse;

import com.google.common.base.Function;
import com.google.common.base.Optional;
import com.google.common.collect.ImmutableList;
import com.squareup.javapoet.TypeName;
import net.zerobuilder.Goal;
import net.zerobuilder.Step;
import net.zerobuilder.compiler.analyse.DtoGoalElement.GoalElementCases;
import net.zerobuilder.compiler.analyse.DtoValidParameter.ValidBeanParameter;
import net.zerobuilder.compiler.analyse.DtoValidParameter.ValidBeanParameter.CollectionType;
import net.zerobuilder.compiler.analyse.DtoValidParameter.ValidRegularParameter;
import net.zerobuilder.compiler.analyse.DtoValidGoal.ValidGoal;

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
import static net.zerobuilder.compiler.analyse.DtoGoalElement.goalElementCases;
import static net.zerobuilder.compiler.analyse.ProjectionValidatorB.validateBean;
import static net.zerobuilder.compiler.analyse.ProjectionValidatorV.validateValue;
import static net.zerobuilder.compiler.analyse.ProjectionValidatorV.validateValueSkipProjections;

public final class ProjectionValidator {

  static final GoalElementCases<ValidGoal> validate = goalElementCases(validateValue, validateBean);
  static final GoalElementCases<ValidGoal> skip = goalElementCases(validateValueSkipProjections, validateBean);

  static <E extends TmpValidParameter> ImmutableList<E> shuffledParameters(ImmutableList<E> parameters)
      throws ValidationException {
    List<E> builder = new ArrayList<>(nCopies(parameters.size(), (E) null));
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
      private final ValidRegularParameter parameter;
      private TmpRegularParameter(Element element, Optional<Step> annotation, ValidRegularParameter parameter) {
        super(element, annotation);
        this.parameter = parameter;
      }
      static final Function<TmpRegularParameter, ValidRegularParameter> toValidParameter = new Function<TmpRegularParameter, ValidRegularParameter>() {
        @Override
        public ValidRegularParameter apply(TmpRegularParameter parameter) {
          return parameter.parameter;
        }
      };
      static TmpRegularParameter create(VariableElement element, Optional<String> projectionMethodName,
                                        Goal goalAnnotation) {
        Step stepAnnotation = element.getAnnotation(Step.class);
        boolean nonNull = TmpValidParameter.nonNull(element.asType(), stepAnnotation, goalAnnotation);
        ValidRegularParameter parameter = new ValidRegularParameter(element.getSimpleName().toString(),
            TypeName.get(element.asType()), projectionMethodName, nonNull);
        return new TmpRegularParameter(element, fromNullable(stepAnnotation), parameter);
      }
    }

    private static boolean nonNull(TypeMirror type, Step step, Goal goal) {
      if (TypeName.get(type).isPrimitive()) {
        return false;
      }
      if (step != null) {
        return step.nonNull();
      }
      return goal.nonNull();
    }

    static final class TmpAccessorPair extends TmpValidParameter {
      final ValidBeanParameter validBeanParameter;
      private TmpAccessorPair(Element element, Optional<Step> annotation, ValidBeanParameter validBeanParameter) {
        super(element, annotation);
        this.validBeanParameter = validBeanParameter;
      }
      static final Function<TmpAccessorPair, ValidBeanParameter> toValidParameter = new Function<TmpAccessorPair, ValidBeanParameter>() {
        @Override
        public ValidBeanParameter apply(TmpAccessorPair parameter) {
          return parameter.validBeanParameter;
        }
      };
      static TmpAccessorPair create(ExecutableElement getter, CollectionType collectionType, Goal goalAnnotation) {
        Step stepAnnotation = getter.getAnnotation(Step.class);
        boolean nonNull = TmpValidParameter.nonNull(getter.getReturnType(), stepAnnotation, goalAnnotation);
        ValidBeanParameter validBeanParameter = new ValidBeanParameter(TypeName.get(getter.getReturnType()),
            getter.getSimpleName().toString(), collectionType, nonNull);
        return new TmpAccessorPair(getter, fromNullable(stepAnnotation), validBeanParameter);
      }
      static TmpAccessorPair createRegular(ExecutableElement getter, Goal goalAnnotation) {
        return create(getter, CollectionType.ABSENT, goalAnnotation);
      }

    }

    private TmpValidParameter(Element element, Optional<Step> annotation) {
      this.element = element;
      this.annotation = annotation;
    }

  }

  private ProjectionValidator() {
    throw new UnsupportedOperationException("no instances");
  }
}
