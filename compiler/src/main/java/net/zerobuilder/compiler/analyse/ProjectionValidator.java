package net.zerobuilder.compiler.analyse;

import java.util.ArrayList;
import java.util.List;
import javax.lang.model.element.Element;
import javax.lang.model.element.VariableElement;
import net.zerobuilder.StepOrder;
import net.zerobuilder.compiler.generate.ProjectedParameter;

import static java.util.Collections.nCopies;
import static javax.tools.Diagnostic.Kind.ERROR;
import static net.zerobuilder.compiler.Messages.STEP_DUPLICATE;
import static net.zerobuilder.compiler.Messages.STEP_OUT_OF_BOUNDS;

final class ProjectionValidator {

  /**
   * Modifies the parameter order, depending on {@link StepOrder} annotations.
   * If none of the parameters has a {@link StepOrder} annotation, the
   * order of the input parameters is not changed.
   *
   * @param parameters parameters in original order
   * @return parameters in a potentially different order
   * @throws ValidationException if the input is inconsistent
   */
  static List<TmpProjectedParameter> shuffledParameters(List<TmpProjectedParameter> parameters)
      throws ValidationException {
    List<TmpProjectedParameter> builder = new ArrayList<>(nCopies(parameters.size(), null));
    List<TmpProjectedParameter> noOrder = new ArrayList<>(parameters.size());
    for (TmpProjectedParameter parameter : parameters) {
      int stepOrder = parameter.stepOrder();
      if (stepOrder >= 0) {
        parameter.checkState(stepOrder < parameters.size(), STEP_OUT_OF_BOUNDS);
        parameter.checkState(builder.get(stepOrder) == null, STEP_DUPLICATE);
        builder.set(stepOrder, parameter);
      } else {
        noOrder.add(parameter);
      }
    }
    int pos = 0;
    for (TmpProjectedParameter parameter : noOrder) {
      while (builder.get(pos) != null) {
        pos++;
      }
      builder.set(pos++, parameter);
    }
    return builder;
  }

  record TmpProjectedParameter(
      Element element,
      int stepOrder,
      ProjectedParameter parameter) {

    static TmpProjectedParameter create(ProjectedParameter projectedParameter) {
      VariableElement parameter = projectedParameter.parameter();
      StepOrder stepOrderAnnotation = parameter.getAnnotation(StepOrder.class);
      int stepOrder = stepOrderAnnotation == null ? -1 : stepOrderAnnotation.value();
      return new TmpProjectedParameter(parameter, stepOrder, projectedParameter);
    }

    void checkState(boolean condition, String message) {
      if (!condition) {
        throw new ValidationException(ERROR, message, element);
      }
    }
  }

  private ProjectionValidator() {
  }
}
