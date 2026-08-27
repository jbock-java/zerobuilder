package net.zerobuilder.compiler.analyse;

import com.palantir.javapoet.ClassName;

import java.util.List;
import javax.lang.model.element.ExecutableElement;
import javax.lang.model.element.TypeElement;

import net.zerobuilder.StepName;
import net.zerobuilder.compiler.generate.Access;
import net.zerobuilder.compiler.generate.GoalDetails;

import static net.zerobuilder.compiler.generate.ZeroUtil.transform;

public final class DtoGoalElement {

  static List<String> parameterNames(ExecutableElement element) {
    return transform(element.getParameters(),
        parameter -> {
          StepName stepNameAnnotation = parameter.getAnnotation(StepName.class);
          return stepNameAnnotation == null ? parameter.getSimpleName().toString() : stepNameAnnotation.value();
        });
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

  private DtoGoalElement() {
  }
}
