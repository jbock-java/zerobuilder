package net.zerobuilder.compiler.analyse;

import com.squareup.javapoet.ClassName;
import com.squareup.javapoet.TypeName;
import com.squareup.javapoet.TypeVariableName;
import net.zerobuilder.Access;
import net.zerobuilder.BeanBuilder;
import net.zerobuilder.Builder;
import net.zerobuilder.Updater;
import net.zerobuilder.compiler.generate.DtoContext;
import net.zerobuilder.compiler.generate.DtoGoalDetails.AbstractGoalDetails;
import net.zerobuilder.compiler.generate.DtoGoalDetails.AbstractRegularDetails;
import net.zerobuilder.compiler.generate.DtoGoalDetails.BeanGoalDetails;
import net.zerobuilder.compiler.generate.DtoGoalDetails.ConstructorGoalDetails;
import net.zerobuilder.compiler.generate.DtoGoalDetails.InstanceMethodGoalDetails;
import net.zerobuilder.compiler.generate.DtoGoalDetails.StaticMethodGoalDetails;

import javax.lang.model.element.Element;
import javax.lang.model.element.ExecutableElement;
import javax.lang.model.element.TypeElement;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.function.Function;

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

  interface GoalElementCases<R> {
    R regularGoal(AbstractRegularGoalElement goal);
    R beanGoal(BeanGoalElement goal);
  }

  private static <R> Function<AbstractGoalElement, R> asFunction(GoalElementCases<R> cases) {
    return goal -> goal.accept(cases);
  }

  interface AbstractGoalElement {
    <R> R accept(GoalElementCases<R> goalElementCases);
  }

  interface RegularGoalElementCases<R> {
    R general(RegularGoalElement regular);
    R projectable(RegularProjectableGoalElement projectable);
  }

  interface AbstractRegularGoalElement extends AbstractGoalElement {
    <R> R accept(RegularGoalElementCases<R> cases);
  }

  private static <R> Function<AbstractRegularGoalElement, R> asFunction(RegularGoalElementCases<R> cases) {
    return element -> element.accept(cases);
  }

  static <R> Function<AbstractRegularGoalElement, R> regularGoalElementCases(
      Function<? super RegularGoalElement, ? extends R> generalFunction,
      Function<? super RegularProjectableGoalElement, ? extends R> projectableFunction) {
    return asFunction(new RegularGoalElementCases<R>() {
      @Override
      public R general(RegularGoalElement regular) {
        return generalFunction.apply(regular);
      }
      @Override
      public R projectable(RegularProjectableGoalElement projectable) {
        return projectableFunction.apply(projectable);
      }
    });
  }

  static final Function<AbstractRegularGoalElement, ExecutableElement> executableElement =
      regularGoalElementCases(
          general -> general.executableElement,
          projectable -> projectable.executableElement);

  static <R> Function<AbstractGoalElement, R> goalElementCases(
      Function<? super AbstractRegularGoalElement, ? extends R> regularGoalFunction,
      Function<? super BeanGoalElement, ? extends R> beanGoalFunction) {
    return asFunction(new GoalElementCases<R>() {
      @Override
      public R regularGoal(AbstractRegularGoalElement executableGoal) {
        return regularGoalFunction.apply(executableGoal);
      }

      @Override
      public R beanGoal(BeanGoalElement beanGoal) {
        return beanGoalFunction.apply(beanGoal);
      }
    });
  }

  private static final Function<AbstractRegularGoalElement, AbstractGoalDetails> abstractDetails =
      regularGoalElementCases(
          general -> general.details,
          projectable -> projectable.details);

  static final Function<AbstractGoalElement, String> goalName =
      goalElementCases(
          regular -> abstractDetails.apply(regular).name(),
          bean -> bean.details.name());

  static final Function<AbstractGoalElement, Element> element =
      goalElementCases(
          regularGoalElementCases(
              regular -> regular.executableElement,
              projected -> projected.executableElement),
          bean -> bean.beanType);

  static final class RegularGoalElement implements AbstractRegularGoalElement {
    final AbstractRegularDetails details;
    final ExecutableElement executableElement;
    final GoalModifiers goalAnnotation;
    final DtoContext.GoalContext context;

    private RegularGoalElement(ExecutableElement element, AbstractRegularDetails details,
                               DtoContext.GoalContext context) {
      this.goalAnnotation = GoalModifiers.create(element);
      this.details = details;
      this.executableElement = element;
      this.context = context;
    }

    @Override
    public <R> R accept(GoalElementCases<R> goalElementCases) {
      return goalElementCases.regularGoal(this);
    }

    @Override
    public <R> R accept(RegularGoalElementCases<R> cases) {
      return cases.general(this);
    }
  }

  static final class RegularProjectableGoalElement implements AbstractRegularGoalElement {
    final AbstractRegularDetails details;
    final ExecutableElement executableElement;
    final GoalModifiers goalAnnotation;

    private RegularProjectableGoalElement(ExecutableElement element, AbstractRegularDetails details) {
      this.goalAnnotation = GoalModifiers.create(element);
      this.details = details;
      this.executableElement = element;
    }

    @Override
    public <R> R accept(GoalElementCases<R> goalElementCases) {
      return goalElementCases.regularGoal(this);
    }

    @Override
    public <R> R accept(RegularGoalElementCases<R> cases) {
      return cases.projectable(this);
    }
  }

  private static List<String> parameterNames(ExecutableElement element) {
    return transform(element.getParameters(),
        parameter -> parameter.getSimpleName().toString());
  }

  static final class BeanGoalElement implements AbstractGoalElement {
    final BeanGoalDetails details;
    final TypeElement beanType;
    final ModuleChoice moduleChoice;

    private BeanGoalElement(ClassName goalType, String name, TypeElement beanType,
                            ModuleChoice moduleChoice, DtoContext.GoalContext context) {
      this.moduleChoice = moduleChoice;
      this.details = new BeanGoalDetails(goalType, name, Access.PUBLIC, context);
      this.beanType = beanType;
    }

    static List<BeanGoalElement> create(TypeElement beanType, DtoContext.GoalContext context) {
      ClassName goalType = ClassName.get(beanType);
      String name = downcase(simpleName(goalType));
      List<ModuleChoice> goalOptions = Arrays.asList(BUILDER, UPDATER);
      return transform(goalOptions,
          goalOption -> new BeanGoalElement(goalType, name, beanType, goalOption, context));
    }

    @Override
    public <R> R accept(GoalElementCases<R> goalElementCases) {
      return goalElementCases.beanGoal(this);
    }
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
    switch (goal.getKind()) {
      case CONSTRUCTOR:
        return ClassName.get(goal.getEnclosingElement().asType());
      default:
        return TypeName.get(goal.getReturnType());
    }
  }

  static Function<ExecutableElement, List<AbstractRegularGoalElement>> createRegular(DtoContext.GoalContext context) {
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
                      parameterNames(element)));
    };
  }

  private static AbstractRegularGoalElement createUpdaterGoal(ExecutableElement element, TypeName goalType,
                                                              GoalModifiers goalModifiers,
                                                              String methodName,
                                                              List<String> parameterNames) {
    if (element.getKind() == CONSTRUCTOR) {
      return new RegularProjectableGoalElement(element, ConstructorGoalDetails.create(
          ClassName.get(asTypeElement(element.getEnclosingElement().asType())),
          goalModifiers.goalName, parameterNames, goalModifiers.access, instanceTypevars(element),
          goalModifiers.lifecycle));
    }
    AbstractRegularDetails details =
        element.getModifiers().contains(STATIC) ?
            StaticMethodGoalDetails.create(goalType, goalModifiers.goalName, parameterNames, methodName,
                goalModifiers.access, methodTypevars(element), goalModifiers.lifecycle) :
            InstanceMethodGoalDetails.create(goalType, goalModifiers.goalName, parameterNames, methodName,
                goalModifiers.access,
                methodTypevars(element),
                instanceTypevars(element),
                returnTypeInstanceTypevars(element),
                goalModifiers.lifecycle);
    return new RegularProjectableGoalElement(element, details);
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

  private static AbstractRegularGoalElement createBuilderGoal(ExecutableElement element, TypeName goalType,
                                                              GoalModifiers goalModifiers,
                                                              String methodName,
                                                              List<String> parameterNames,
                                                              DtoContext.GoalContext context) {
    if (element.getKind() == CONSTRUCTOR) {
      ConstructorGoalDetails details = ConstructorGoalDetails.create(
          ClassName.get(asTypeElement(element.getEnclosingElement().asType())),
          goalModifiers.goalName, parameterNames, goalModifiers.access, instanceTypevars(element),
          goalModifiers.lifecycle);
      return new RegularGoalElement(element, details, context);
    }
    AbstractRegularDetails details =
        element.getModifiers().contains(STATIC) ?
            StaticMethodGoalDetails.create(goalType, goalModifiers.goalName, parameterNames, methodName,
                goalModifiers.access,
                methodTypevars(element), goalModifiers.lifecycle) :
            InstanceMethodGoalDetails.create(goalType, goalModifiers.goalName, parameterNames, methodName,
                goalModifiers.access,
                methodTypevars(element),
                instanceTypevars(element),
                returnTypeInstanceTypevars(element),
                goalModifiers.lifecycle);
    return new RegularGoalElement(element, details, context);
  }

  private DtoGoalElement() {
    throw new UnsupportedOperationException("no instances");
  }
}
