package net.zerobuilder.compiler.analyse;

import com.squareup.javapoet.TypeName;
import net.zerobuilder.Goal;
import net.zerobuilder.NullPolicy;
import net.zerobuilder.Step;
import net.zerobuilder.compiler.analyse.DtoGoalElement.GoalElementCases;
import net.zerobuilder.compiler.generate.DtoBeanParameter.AbstractBeanParameter;
import net.zerobuilder.compiler.generate.DtoBeanParameter.AccessorPair;
import net.zerobuilder.compiler.generate.DtoBeanParameter.LoneGetter;
import net.zerobuilder.compiler.generate.DtoGoalDescription.GoalDescription;
import net.zerobuilder.compiler.generate.DtoParameter.RegularParameter;

import javax.lang.model.element.Element;
import javax.lang.model.element.ExecutableElement;
import javax.lang.model.element.VariableElement;
import javax.lang.model.type.TypeMirror;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.function.Function;

import static java.util.Collections.nCopies;
import static java.util.Optional.ofNullable;
import static javax.tools.Diagnostic.Kind.ERROR;
import static net.zerobuilder.compiler.Messages.ErrorMessages.DUPLICATE_STEP_POSITION;
import static net.zerobuilder.compiler.Messages.ErrorMessages.STEP_POSITION_TOO_LARGE;
import static net.zerobuilder.compiler.analyse.DtoGoalElement.goalElementCases;
import static net.zerobuilder.compiler.analyse.ProjectionValidatorB.validateBean;
import static net.zerobuilder.compiler.analyse.ProjectionValidatorV.validateValue;
import static net.zerobuilder.compiler.analyse.ProjectionValidatorV.validateValueSkipProjections;

final class ProjectionValidator {

  static final GoalElementCases<GoalDescription> validate = goalElementCases(validateValue, validateBean);
  static final GoalElementCases<GoalDescription> skip = goalElementCases(validateValueSkipProjections, validateBean);

  static <E extends TmpValidParameter> List<E> shuffledParameters(List<E> parameters)
      throws ValidationException {
    List<E> builder = new ArrayList<>(nCopies(parameters.size(), (E) null));
    List<E> noAnnotation = new ArrayList<>();
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
    for (E parameter : noAnnotation) {
      while (builder.get(pos) != null) {
        pos++;
      }
      builder.set(pos++, parameter);
    }
    return builder;
  }

  static abstract class TmpValidParameter {

    final Element element;
    final Optional<Step> annotation;

    static boolean nonNull(TypeMirror type, Step step, Goal goal) {
      if (TypeName.get(type).isPrimitive()) {
        return false;
      }
      NullPolicy defaultPolicy = goal.nullPolicy() == NullPolicy.DEFAULT
          ? NullPolicy.ALLOW
          : goal.nullPolicy();
      if (step != null) {
        return step.nullPolicy() == NullPolicy.DEFAULT
            ? defaultPolicy.check()
            : step.nullPolicy().check();
      }
      return defaultPolicy.check();
    }

    private TmpValidParameter(Element element, Optional<Step> annotation) {
      this.element = element;
      this.annotation = annotation;
    }
  }

  static final class TmpRegularParameter extends TmpValidParameter {
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
    static TmpRegularParameter create(VariableElement parameter, Optional<String> getter,
                                      Goal goalAnnotation) {
      Step stepAnnotation = parameter.getAnnotation(Step.class);
      boolean nonNull = TmpValidParameter.nonNull(parameter.asType(), stepAnnotation, goalAnnotation);
      String name = parameter.getSimpleName().toString();
      TypeName type = TypeName.get(parameter.asType());
      RegularParameter regularParameter = createRegularParameter(getter, nonNull, name, type);
      return new TmpRegularParameter(parameter, ofNullable(stepAnnotation), regularParameter);
    }

    private static RegularParameter createRegularParameter(Optional<String> getter, boolean nonNull, String name, TypeName type) {
      if (getter.isPresent()) {
        return RegularParameter.create(name, type, nonNull, getter.get());
      } else {
        return RegularParameter.create(name, type, nonNull);
      }
    }
  }

  static final class TmpAccessorPair extends TmpValidParameter {
    final AbstractBeanParameter validBeanParameter;
    private TmpAccessorPair(Element element, Optional<Step> annotation, AbstractBeanParameter validBeanParameter) {
      super(element, annotation);
      this.validBeanParameter = validBeanParameter;
    }

    static final Function<TmpAccessorPair, AbstractBeanParameter> toValidParameter = parameter -> parameter.validBeanParameter;

    static TmpAccessorPair createAccessorPair(ExecutableElement getter, Goal goalAnnotation) {
      Step stepAnnotation = getter.getAnnotation(Step.class);
      TypeName type = TypeName.get(getter.getReturnType());
      boolean nonNull = TmpValidParameter.nonNull(getter.getReturnType(), stepAnnotation, goalAnnotation);
      AccessorPair accessorPair = AccessorPair.create(type, getter, nonNull);
      return new TmpAccessorPair(getter, ofNullable(stepAnnotation), accessorPair);
    }

    static TmpAccessorPair createLoneGetter(ExecutableElement getter, LoneGetter loneGetter) {
      Step stepAnnotation = getter.getAnnotation(Step.class);
      return new TmpAccessorPair(getter, ofNullable(stepAnnotation), loneGetter);
    }
  }

  private ProjectionValidator() {
    throw new UnsupportedOperationException("no instances");
  }
}
