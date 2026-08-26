package net.zerobuilder.compiler.analyse;

import com.palantir.javapoet.TypeName;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Function;
import javax.lang.model.element.Element;
import javax.lang.model.element.VariableElement;
import net.zerobuilder.StepName;
import net.zerobuilder.StepOrder;
import net.zerobuilder.compiler.generate.DtoProjectionInfo.ProjectionInfo;
import net.zerobuilder.compiler.generate.ProjectedParameter;

import static java.util.Collections.nCopies;
import static javax.tools.Diagnostic.Kind.ERROR;
import static net.zerobuilder.compiler.Messages.ErrorMessages.STEP_DUPLICATE;
import static net.zerobuilder.compiler.Messages.ErrorMessages.STEP_OUT_OF_BOUNDS;

final class ProjectionValidator {

  /**
   * Modifies the parameter order, depending on {@link StepOrder} annotations.
   * If none of the parameters has a {@link StepOrder} annotation, the
   * order of the input parameters is not changed.
   *
   * @param parameters parameters in original order
   * @param <E>        parameter type
   * @return parameters in a potentially different order
   * @throws ValidationException if the input is inconsistent
   */
  static <E extends TmpValidParameter> List<E> shuffledParameters(List<E> parameters)
      throws ValidationException {
    List<E> builder = new ArrayList<>(nCopies(parameters.size(), null));
    List<E> noAnnotation = new ArrayList<>();
    for (E parameter : parameters) {
      int value = parameter.annotation;
      if (value >= 0) {
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
    // step position
    final int annotation;

    private TmpValidParameter(Element element, int annotation) {
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

  static final class TmpProjectedParameter extends TmpValidParameter {
    private final ProjectedParameter parameter;

    private TmpProjectedParameter(Element element, int annotation, ProjectedParameter parameter) {
      super(element, annotation);
      this.parameter = parameter;
    }

    static final Function<TmpProjectedParameter, ProjectedParameter> toValidParameter =
        parameter -> parameter.parameter;

    static TmpProjectedParameter create(VariableElement parameter, ProjectionInfo projectionInfo) {
      StepOrder stepOrder = parameter.getAnnotation(StepOrder.class);
      int value = stepOrder == null ? -1 : stepOrder.value();
      StepName stepNameAnnotation = parameter.getAnnotation(StepName.class);
      String name = stepNameAnnotation == null ? parameter.getSimpleName().toString() : stepNameAnnotation.value();
      TypeName type = TypeName.get(parameter.asType());
      ProjectedParameter regularParameter = new ProjectedParameter(name, type, projectionInfo);
      return new TmpProjectedParameter(parameter, value, regularParameter);
    }
  }

  private ProjectionValidator() {
  }
}
