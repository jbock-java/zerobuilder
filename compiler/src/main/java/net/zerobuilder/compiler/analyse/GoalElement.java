package net.zerobuilder.compiler.analyse;

import com.palantir.javapoet.ClassName;
import java.util.ArrayList;
import java.util.List;
import javax.lang.model.element.ExecutableElement;
import javax.lang.model.element.TypeElement;
import javax.lang.model.element.VariableElement;
import net.zerobuilder.StepName;
import net.zerobuilder.StepOrder;
import net.zerobuilder.compiler.generate.Access;
import net.zerobuilder.compiler.generate.GoalDetails;

import static net.zerobuilder.compiler.Messages.STEP_DUPLICATE;
import static net.zerobuilder.compiler.Messages.STEP_OUT_OF_BOUNDS;

record GoalElement(
    GoalDetails details,
    ExecutableElement executableElement,
    ClassName generatedType
) {

  private static List<String> parameterNames(ExecutableElement element)
      throws ValidationException {
    List<? extends VariableElement> parameters = element.getParameters();
    String[] result = new String[parameters.size()];
    List<String> noOrder = new ArrayList<>(parameters.size());
    for (VariableElement parameter : parameters) {
      StepOrder stepOrderAnnotation = parameter.getAnnotation(StepOrder.class);
      int stepOrder = stepOrderAnnotation == null ? -1 : stepOrderAnnotation.value();
      StepName stepNameAnnotation = parameter.getAnnotation(StepName.class);
      String paramName = stepNameAnnotation == null ? parameter.getSimpleName().toString() : stepNameAnnotation.value();
      if (stepOrder >= 0) {
        if (stepOrder >= parameters.size()) {
          throw new ValidationException(STEP_OUT_OF_BOUNDS, element);
        }
        if (result[stepOrder] != null) {
          throw new ValidationException(STEP_DUPLICATE, element);
        }
        result[stepOrder] = paramName;
      } else {
        noOrder.add(paramName);
      }
    }
    int pos = 0;
    for (String parameter : noOrder) {
      while (result[pos] != null) {
        pos++;
      }
      result[pos++] = parameter;
    }
    return List.of(result);
  }

  static GoalElement create(
      TypeElement tel,
      ExecutableElement element,
      ClassName generatedType) {
    Access access = Access.getAccess(tel);
    List<String> parameterNames = parameterNames(element);
    GoalDetails details = new GoalDetails(
        tel,
        parameterNames,
        access);
    return new GoalElement(details, element, generatedType);
  }
}
