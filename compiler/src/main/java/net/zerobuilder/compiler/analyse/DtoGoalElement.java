package net.zerobuilder.compiler.analyse;

import com.palantir.javapoet.ClassName;
import com.palantir.javapoet.TypeName;
import com.palantir.javapoet.TypeVariableName;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.function.Function;
import javax.lang.model.element.Element;
import javax.lang.model.element.ExecutableElement;
import javax.lang.model.element.TypeElement;
import net.zerobuilder.Builder;
import net.zerobuilder.Name;
import net.zerobuilder.Updater;
import net.zerobuilder.compiler.generate.Access;
import net.zerobuilder.compiler.generate.DtoContext;
import net.zerobuilder.compiler.generate.DtoGoalDetails.AbstractRegularDetails;
import net.zerobuilder.compiler.generate.DtoGoalDetails.BeanGoalDetails;
import net.zerobuilder.compiler.generate.DtoGoalDetails.ConstructorGoalDetails;
import net.zerobuilder.compiler.generate.DtoGoalDetails.InstanceMethodGoalDetails;
import net.zerobuilder.compiler.generate.DtoGoalDetails.StaticMethodGoalDetails;

import static java.util.Collections.emptyList;
import static java.util.stream.Collectors.toList;
import static javax.lang.model.element.ElementKind.CONSTRUCTOR;
import static javax.lang.model.element.Modifier.STATIC;
import static net.zerobuilder.compiler.analyse.DtoGoalElement.ModuleChoice.BUILDER;
import static net.zerobuilder.compiler.analyse.DtoGoalElement.ModuleChoice.UPDATER;
import static net.zerobuilder.compiler.common.LessTypes.asTypeElement;
import static net.zerobuilder.compiler.common.LessTypes.isDeclaredType;
import static net.zerobuilder.compiler.generate.ZeroUtil.downcase;
import static net.zerobuilder.compiler.generate.ZeroUtil.simpleName;
import static net.zerobuilder.compiler.generate.ZeroUtil.transform;

final class DtoGoalElement {

  sealed interface AbstractGoalElement permits BeanGoalElement, RegularGoalElement, RegularProjectableGoalElement {
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
      case BeanGoalElement bean -> bean.details.name;
      case RegularGoalElement regular -> regular.details.name();
      case RegularProjectableGoalElement projected -> projected.details.name();
    };
  }

  static Element element(AbstractGoalElement element) {
    return switch (element) {
      case BeanGoalElement bean -> bean.beanType;
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

  static BeanGoalElement createBeanGoalElement(
      ClassName goalType,
      String name,
      TypeElement beanType,
      ModuleChoice moduleChoice,
      DtoContext.GoalContext context
  ) {
    return new BeanGoalElement(new BeanGoalDetails(goalType, name, Access.PUBLIC, context), beanType, moduleChoice);
  }

  static List<BeanGoalElement> createBeanGoalElements(
      TypeElement beanType,
      DtoContext.GoalContext context) {
    ClassName goalType = ClassName.get(beanType);
    String name = downcase(simpleName(goalType));
    List<ModuleChoice> goalOptions = Arrays.asList(BUILDER, UPDATER);
    return transform(goalOptions,
        goalOption -> createBeanGoalElement(goalType, name, beanType, goalOption, context));
  }

  record BeanGoalElement(
      BeanGoalDetails details,
      TypeElement beanType,
      ModuleChoice moduleChoice
  ) implements AbstractGoalElement {
  }

  enum ModuleChoice {
    UPDATER, BUILDER
  }

  private static List<ModuleChoice> goalOptions(ExecutableElement element) {
    ArrayList<ModuleChoice> options = new ArrayList<>(2);
    if (element.getAnnotation(Builder.class) != null) {
      options.add(BUILDER);
    }
    if (element.getAnnotation(Updater.class) != null) {
      options.add(UPDATER);
    }
    return options;
  }

  static TypeName goalType(ExecutableElement goal) {
    if (goal.getKind() == CONSTRUCTOR) {
      return ClassName.get(goal.getEnclosingElement().asType());
    }
    return TypeName.get(goal.getReturnType());
  }

  static Function<ExecutableElement, List<? extends AbstractGoalElement>> createRegular(DtoContext.GoalContext context) {
    return element -> {
      TypeName goalType = goalType(element);
      GoalModifiers modifiers = GoalModifiers.create(element);
      List<ModuleChoice> goalOptions = goalOptions(element);
      String methodName = element.getSimpleName().toString();
      return transform(goalOptions,
          goalOption ->
              goalOption == BUILDER ?
                  createBuilderGoal(element, goalType, modifiers, methodName,
                      parameterNames(element), context) :
                  createUpdaterGoal(element, goalType, modifiers, methodName,
                      parameterNames(element), context));
    };
  }

  private static AbstractGoalElement createUpdaterGoal(
      ExecutableElement element,
      TypeName goalType,
      GoalModifiers goalModifiers,
      String methodName,
      List<String> parameterNames,
      DtoContext.GoalContext context) {
    if (element.getKind() == CONSTRUCTOR) {
      return createRegularProjectableGoalElement(element, ConstructorGoalDetails.create(
          ClassName.get(asTypeElement(element.getEnclosingElement().asType())),
          goalModifiers.goalName, parameterNames, goalModifiers.access, instanceTypevars(element)), context);
    }
    AbstractRegularDetails details =
        element.getModifiers().contains(STATIC) ?
            StaticMethodGoalDetails.create(goalType, goalModifiers.goalName, parameterNames, methodName,
                goalModifiers.access, methodTypevars(element)) :
            InstanceMethodGoalDetails.create(goalType, goalModifiers.goalName, parameterNames, methodName,
                goalModifiers.access,
                methodTypevars(element),
                instanceTypevars(element),
                returnTypeInstanceTypevars(element));
    return createRegularProjectableGoalElement(element, details, context);
  }

  private static List<TypeVariableName> instanceTypevars(ExecutableElement element) {
    TypeElement type = asTypeElement(element.getEnclosingElement().asType());
    return transform(type.getTypeParameters(), TypeVariableName::get);
  }

  private static List<TypeVariableName> returnTypeInstanceTypevars(ExecutableElement element) {
    if (!isDeclaredType(element.getReturnType())) {
      return emptyList();
    }
    TypeElement type = asTypeElement(element.getReturnType());
    return transform(type.getTypeParameters(), TypeVariableName::get);
  }

  private static List<TypeVariableName> methodTypevars(ExecutableElement element) {
    return element.getTypeParameters().stream()
        .map(TypeVariableName::get)
        .collect(toList());
  }

  private static AbstractGoalElement createBuilderGoal(
      ExecutableElement element,
      TypeName goalType,
      GoalModifiers goalModifiers,
      String methodName,
      List<String> parameterNames,
      DtoContext.GoalContext context) {
    if (element.getKind() == CONSTRUCTOR) {
      ConstructorGoalDetails details = ConstructorGoalDetails.create(
          ClassName.get(asTypeElement(element.getEnclosingElement().asType())),
          goalModifiers.goalName, parameterNames, goalModifiers.access, instanceTypevars(element));
      return createRegularGoalElement(element, details, context);
    }
    AbstractRegularDetails details =
        element.getModifiers().contains(STATIC) ?
            StaticMethodGoalDetails.create(goalType, goalModifiers.goalName, parameterNames, methodName,
                goalModifiers.access,
                methodTypevars(element)) :
            InstanceMethodGoalDetails.create(goalType, goalModifiers.goalName, parameterNames, methodName,
                goalModifiers.access,
                methodTypevars(element),
                instanceTypevars(element),
                returnTypeInstanceTypevars(element));
    return createRegularGoalElement(element, details, context);
  }

  private DtoGoalElement() {
    throw new UnsupportedOperationException("no instances");
  }
}
