package net.zerobuilder.compiler.analyse;

import com.palantir.javapoet.ClassName;
import com.palantir.javapoet.TypeName;
import com.palantir.javapoet.TypeVariableName;
import java.util.List;
import javax.lang.model.element.ExecutableElement;
import javax.lang.model.element.TypeElement;
import net.zerobuilder.StepName;
import net.zerobuilder.compiler.generate.GoalContext;
import net.zerobuilder.compiler.generate.GoalDetails;

import static javax.lang.model.element.ElementKind.CONSTRUCTOR;
import static net.zerobuilder.compiler.common.LessTypes.asTypeElement;
import static net.zerobuilder.compiler.generate.GoalDetails.createGoalDetails;
import static net.zerobuilder.compiler.generate.ZeroUtil.transform;

public final class DtoGoalElement {

  static GoalElement createGoalElement(
      ExecutableElement element,
      GoalDetails details,
      GoalContext context) {
    return new GoalElement(details, element, GoalModifiers.create(element), context);
  }

  static List<String> parameterNames(ExecutableElement element) {
    return transform(element.getParameters(),
        parameter -> {
          StepName stepNameAnnotation = parameter.getAnnotation(StepName.class);
          return stepNameAnnotation == null ? parameter.getSimpleName().toString() : stepNameAnnotation.value();
        });
  }

  static TypeName goalType(ExecutableElement goal) {
    if (goal.getKind() == CONSTRUCTOR) {
      return ClassName.get(goal.getEnclosingElement().asType());
    }
    return TypeName.get(goal.getReturnType());
  }

  private static List<TypeVariableName> instanceTypevars(ExecutableElement element) {
    TypeElement type = asTypeElement(element.getEnclosingElement().asType());
    return transform(type.getTypeParameters(), TypeVariableName::get);
  }

  static GoalElement createBuilderGoal(
      ExecutableElement element,
      GoalModifiers goalModifiers,
      List<String> parameterNames,
      GoalContext context) {
    GoalDetails details = createGoalDetails(
        ClassName.get(asTypeElement(element.getEnclosingElement().asType())),
        goalModifiers.goalName, parameterNames, goalModifiers.access, instanceTypevars(element));
    return createGoalElement(element, details, context);
  }

  private DtoGoalElement() {
  }
}
