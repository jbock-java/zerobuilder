package net.zerobuilder.compiler.analyse;

import com.squareup.javapoet.TypeName;
import net.zerobuilder.Goal;
import net.zerobuilder.NullPolicy;
import net.zerobuilder.Step;
import net.zerobuilder.compiler.analyse.DtoGoalElement.AbstractGoalElement;
import net.zerobuilder.compiler.analyse.DtoGoalElement.GoalElementCases;
import net.zerobuilder.compiler.generate.DtoBeanParameter;
import net.zerobuilder.compiler.generate.DtoBeanParameter.AbstractBeanParameter;
import net.zerobuilder.compiler.generate.DtoGoalDescription.GoalDescription;
import net.zerobuilder.compiler.generate.DtoProjectionInfo.ProjectionInfo;
import net.zerobuilder.compiler.generate.DtoRegularParameter;
import net.zerobuilder.compiler.generate.DtoRegularParameter.AbstractRegularParameter;

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
import static net.zerobuilder.compiler.Messages.ErrorMessages.STEP_DUPLICATE;
import static net.zerobuilder.compiler.Messages.ErrorMessages.STEP_OUT_OF_BOUNDS;
import static net.zerobuilder.compiler.analyse.DtoGoalElement.goalElementCases;
import static net.zerobuilder.compiler.analyse.ProjectionValidatorB.validateBean;
import static net.zerobuilder.compiler.analyse.ProjectionValidatorV.validateValue;
import static net.zerobuilder.compiler.analyse.ProjectionValidatorV.validateValueIgnoreProjections;
import static net.zerobuilder.compiler.analyse.Utilities.thrownTypes;

final class ProjectionValidator {

  static final Function<AbstractGoalElement, GoalDescription> validate =
      goalElementCases(validateValue, validateBean);

  static final Function<AbstractGoalElement, GoalDescription> skip =
      goalElementCases(validateValueIgnoreProjections, validateBean);

  /**
   * Modifies the parameter order, depending on {@link Step} annotations.
   * If none of the parameters has a {@link Step} annotation, the
   * order of the input parameters is not changed.
   *
   * @param parameters parameters in original order
   * @param <E>        parameter type
   * @return parameters in a potentially different order
   * @throws ValidationException if the input is inconsistent
   */
  static <E extends TmpValidParameter> List<E> shuffledParameters(List<E> parameters)
      throws ValidationException {
    List<E> builder = new ArrayList<>(nCopies(parameters.size(), (E) null));
    List<E> noAnnotation = new ArrayList<>();
    for (E parameter : parameters) {
      Optional<Step> step = parameter.annotation;
      if (step.isPresent() && step.get().value() >= 0) {
        int value = step.get().value();
        parameter
            .checkState(value < parameters.size(), STEP_OUT_OF_BOUNDS)
            .checkState(builder.get(value) == null, STEP_DUPLICATE);
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

    static NullPolicy nullPolicy(TypeMirror type, Step step, Goal goal) {
      if (TypeName.get(type).isPrimitive()) {
        return NullPolicy.ALLOW;
      }
      NullPolicy defaultPolicy = goal.nullPolicy() == NullPolicy.DEFAULT
          ? NullPolicy.ALLOW
          : goal.nullPolicy();
      if (step != null) {
        return step.nullPolicy() == NullPolicy.DEFAULT
            ? defaultPolicy
            : step.nullPolicy();
      }
      return defaultPolicy;
    }

    private TmpValidParameter(Element element, Optional<Step> annotation) {
      this.element = element;
      this.annotation = annotation;
    }

    TmpValidParameter checkState(boolean condition, String message) {
      if (!condition) {
        throw new ValidationException(ERROR,
            message, element);
      }
      return this;
    }

  }

  static final class TmpRegularParameter extends TmpValidParameter {
    private final AbstractRegularParameter parameter;
    private TmpRegularParameter(Element element, Optional<Step> annotation, AbstractRegularParameter parameter) {
      super(element, annotation);
      this.parameter = parameter;
    }

    static final Function<TmpRegularParameter, AbstractRegularParameter> toValidParameter =
        parameter -> parameter.parameter;

    static TmpRegularParameter create(VariableElement parameter, Optional<ProjectionInfo> projectionInfo,
                                      Goal goalAnnotation) {
      Step stepAnnotation = parameter.getAnnotation(Step.class);
      NullPolicy nullPolicy = TmpValidParameter.nullPolicy(parameter.asType(), stepAnnotation, goalAnnotation);
      String name = parameter.getSimpleName().toString();
      TypeName type = TypeName.get(parameter.asType());
      AbstractRegularParameter regularParameter =
          projectionInfo.isPresent() ?
              DtoRegularParameter.create(name, type, nullPolicy, projectionInfo.get()) :
              DtoRegularParameter.create(name, type, nullPolicy);
      return new TmpRegularParameter(parameter, ofNullable(stepAnnotation), regularParameter);
    }
  }

  static final class TmpAccessorPair extends TmpValidParameter {
    final AbstractBeanParameter parameter;
    private TmpAccessorPair(Element element, Optional<Step> annotation, AbstractBeanParameter parameter) {
      super(element, annotation);
      this.parameter = parameter;
    }

    static final Function<TmpAccessorPair, AbstractBeanParameter> toValidParameter = parameter -> parameter.parameter;

    static TmpAccessorPair accessorPair(ExecutableElement getter, ExecutableElement setter, Goal goalAnnotation) {
      Step stepAnnotation = getter.getAnnotation(Step.class);
      TypeName type = TypeName.get(getter.getReturnType());
      NullPolicy nullPolicy = TmpValidParameter.nullPolicy(getter.getReturnType(), stepAnnotation, goalAnnotation);
      AbstractBeanParameter accessorPair = DtoBeanParameter.accessorPair(type, getter.getSimpleName().toString(), nullPolicy,
          thrownTypes(getter), thrownTypes(setter));
      return new TmpAccessorPair(getter, ofNullable(stepAnnotation), accessorPair);
    }

    static TmpAccessorPair createLoneGetter(ExecutableElement getter, AbstractBeanParameter loneGetter) {
      Step stepAnnotation = getter.getAnnotation(Step.class);
      return new TmpAccessorPair(getter, ofNullable(stepAnnotation), loneGetter);
    }
  }

  private ProjectionValidator() {
    throw new UnsupportedOperationException("no instances");
  }
}
