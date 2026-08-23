package net.zerobuilder.compiler.analyse;

import com.palantir.javapoet.ClassName;
import com.palantir.javapoet.TypeName;
import com.palantir.javapoet.TypeVariableName;
import java.util.List;
import javax.lang.model.element.Element;
import javax.lang.model.element.ExecutableElement;
import javax.lang.model.element.TypeElement;
import net.zerobuilder.Name;
import net.zerobuilder.compiler.generate.GoalContext;
import net.zerobuilder.compiler.generate.GoalDetails;

import static javax.lang.model.element.ElementKind.CONSTRUCTOR;
import static net.zerobuilder.compiler.analyse.DtoGoalElement.ModuleChoice.BUILDER;
import static net.zerobuilder.compiler.common.LessTypes.asTypeElement;
import static net.zerobuilder.compiler.generate.GoalDetails.createGoalDetails;
import static net.zerobuilder.compiler.generate.ZeroUtil.transform;

final class DtoGoalElement {

  sealed interface AbstractGoalElement permits BuilderGoalElement, UpdaterGoalElement {
  }

  static ExecutableElement executableElement(AbstractGoalElement element) {
    return switch (element) {
      case BuilderGoalElement regular -> regular.executableElement;
      case UpdaterGoalElement projected -> projected.executableElement;
    };
  }

  static String goalName(AbstractGoalElement element) {
    return switch (element) {
      case BuilderGoalElement regular -> regular.details.name();
      case UpdaterGoalElement projected -> projected.details.name();
    };
  }

  static Element element(AbstractGoalElement element) {
    if (element == null) {
      return null;
    }
    return switch (element) {
      case BuilderGoalElement regular -> regular.executableElement;
      case UpdaterGoalElement projected -> projected.executableElement;
    };
  }

  private static BuilderGoalElement createBuilderGoalElement(
      ExecutableElement element,
      GoalDetails details,
      GoalContext context) {
    return new BuilderGoalElement(details, element, GoalModifiers.create(element), context);
  }

  record BuilderGoalElement(
      GoalDetails details,
      ExecutableElement executableElement,
      GoalModifiers goalAnnotation,
      GoalContext context
  ) implements AbstractGoalElement {
  }

  private static UpdaterGoalElement createUpdaterGoalElement(
      ExecutableElement element,
      GoalDetails details,
      GoalContext context) {
    return new UpdaterGoalElement(details, element, GoalModifiers.create(element), context);
  }

  record UpdaterGoalElement(
      GoalDetails details,
      ExecutableElement executableElement,
      GoalModifiers goalAnnotation,
      GoalContext context
  ) implements AbstractGoalElement {
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
      GoalContext context,
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
      GoalContext context) {
    return createUpdaterGoalElement(element, createGoalDetails(
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
      GoalContext context) {
    GoalDetails details = createGoalDetails(
        ClassName.get(asTypeElement(element.getEnclosingElement().asType())),
        goalModifiers.goalName, parameterNames, goalModifiers.access, instanceTypevars(element));
    return createBuilderGoalElement(element, details, context);
  }

  private DtoGoalElement() {
    throw new UnsupportedOperationException("no instances");
  }
}
