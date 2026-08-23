package net.zerobuilder.compiler.analyse;

import com.palantir.javapoet.ClassName;
import com.palantir.javapoet.TypeName;
import com.palantir.javapoet.TypeVariableName;
import java.util.List;
import javax.lang.model.element.Element;
import javax.lang.model.element.ExecutableElement;
import javax.lang.model.element.TypeElement;
import net.zerobuilder.Name;
import net.zerobuilder.compiler.generate.DtoContext;
import net.zerobuilder.compiler.generate.DtoGoalDetails.AbstractRegularDetails;

import static javax.lang.model.element.ElementKind.CONSTRUCTOR;
import static net.zerobuilder.compiler.analyse.DtoGoalElement.ModuleChoice.BUILDER;
import static net.zerobuilder.compiler.common.LessTypes.asTypeElement;
import static net.zerobuilder.compiler.generate.ZeroUtil.transform;

final class DtoGoalElement {

  sealed interface AbstractGoalElement permits RegularGoalElement, RegularProjectableGoalElement {
  }

  sealed interface AbstractRegularGoalElement permits RegularGoalElement, RegularProjectableGoalElement {
  }

  static ExecutableElement executableElement(AbstractRegularGoalElement element) {
    return switch (element) {
      case RegularGoalElement regular -> regular.executableElement;
      case RegularProjectableGoalElement projected -> projected.executableElement;
    };
  }

  static String goalName(AbstractGoalElement element) {
    return switch (element) {
      case RegularGoalElement regular -> regular.details.name();
      case RegularProjectableGoalElement projected -> projected.details.name();
    };
  }

  static Element element(AbstractGoalElement element) {
    if (element == null) {
      return null;
    }
    return switch (element) {
      case RegularGoalElement regular -> regular.executableElement;
      case RegularProjectableGoalElement projected -> projected.executableElement;
    };
  }

  private static RegularGoalElement createRegularGoalElement(
      ExecutableElement element,
      AbstractRegularDetails details,
      DtoContext.GoalContext context) {
    return new RegularGoalElement(details, element, GoalModifiers.create(element), context);
  }

  record RegularGoalElement(
      AbstractRegularDetails details,
      ExecutableElement executableElement,
      GoalModifiers goalAnnotation,
      DtoContext.GoalContext context
  ) implements AbstractGoalElement, AbstractRegularGoalElement {
  }

  private static RegularProjectableGoalElement createRegularProjectableGoalElement(
      ExecutableElement element,
      AbstractRegularDetails details,
      DtoContext.GoalContext context) {
    return new RegularProjectableGoalElement(details, element, GoalModifiers.create(element), context);
  }

  record RegularProjectableGoalElement(
      AbstractRegularDetails details,
      ExecutableElement executableElement,
      GoalModifiers goalAnnotation,
      DtoContext.GoalContext context
  ) implements AbstractGoalElement, AbstractRegularGoalElement {
  }

  private static List<String> parameterNames(ExecutableElement element) {
    return transform(element.getParameters(),
        parameter -> {
          Name nameAnnotation = parameter.getAnnotation(Name.class);
          return nameAnnotation == null ? parameter.getSimpleName().toString() : nameAnnotation.value();
        });
  }

  enum ModuleChoice {
    UPDATER, BUILDER
  }

  static TypeName goalType(ExecutableElement goal) {
    if (goal.getKind() == CONSTRUCTOR) {
      return ClassName.get(goal.getEnclosingElement().asType());
    }
    return TypeName.get(goal.getReturnType());
  }

  static List<? extends AbstractGoalElement> createRegular(
      DtoContext.GoalContext context,
      ExecutableElement element,
      List<ModuleChoice> goalOptions) {
    GoalModifiers modifiers = GoalModifiers.create(element);
    return transform(goalOptions,
        goalOption ->
            goalOption == BUILDER ?
                createBuilderGoal(element, modifiers,
                    parameterNames(element), context) :
                createUpdaterGoal(element, modifiers,
                    parameterNames(element), context));
  }

  private static AbstractGoalElement createUpdaterGoal(
      ExecutableElement element,
      GoalModifiers goalModifiers,
      List<String> parameterNames,
      DtoContext.GoalContext context) {
    return createRegularProjectableGoalElement(element, AbstractRegularDetails.create(
        ClassName.get(asTypeElement(element.getEnclosingElement().asType())),
        goalModifiers.goalName, parameterNames, goalModifiers.access, instanceTypevars(element)), context);
  }

  private static List<TypeVariableName> instanceTypevars(ExecutableElement element) {
    TypeElement type = asTypeElement(element.getEnclosingElement().asType());
    return transform(type.getTypeParameters(), TypeVariableName::get);
  }

  private static AbstractGoalElement createBuilderGoal(
      ExecutableElement element,
      GoalModifiers goalModifiers,
      List<String> parameterNames,
      DtoContext.GoalContext context) {
    AbstractRegularDetails details = AbstractRegularDetails.create(
        ClassName.get(asTypeElement(element.getEnclosingElement().asType())),
        goalModifiers.goalName, parameterNames, goalModifiers.access, instanceTypevars(element));
    return createRegularGoalElement(element, details, context);
  }

  private DtoGoalElement() {
    throw new UnsupportedOperationException("no instances");
  }
}
