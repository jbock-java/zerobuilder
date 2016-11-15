package net.zerobuilder.compiler.analyse;

import com.squareup.javapoet.ClassName;
import com.squareup.javapoet.TypeName;
import com.squareup.javapoet.TypeVariableName;
import net.zerobuilder.AccessLevel;
import net.zerobuilder.Goal;
import net.zerobuilder.compiler.generate.Access;
import net.zerobuilder.compiler.generate.DtoGoalDetails.AbstractGoalDetails;
import net.zerobuilder.compiler.generate.DtoGoalDetails.AbstractRegularDetails;
import net.zerobuilder.compiler.generate.DtoGoalDetails.BeanGoalDetails;
import net.zerobuilder.compiler.generate.DtoGoalDetails.ConstructorGoalDetails;
import net.zerobuilder.compiler.generate.DtoGoalDetails.InstanceMethodGoalDetails;
import net.zerobuilder.compiler.generate.DtoGoalDetails.ProjectableDetails;
import net.zerobuilder.compiler.generate.DtoGoalDetails.StaticMethodGoalDetails;
import net.zerobuilder.compiler.generate.DtoGoalDetails.StaticMethodGoalDetails.DetailsType;

import javax.lang.model.element.Element;
import javax.lang.model.element.ExecutableElement;
import javax.lang.model.element.TypeElement;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Function;
import java.util.function.Predicate;

import static java.util.Collections.emptyList;
import static java.util.stream.Collectors.toList;
import static javax.lang.model.element.ElementKind.CONSTRUCTOR;
import static javax.lang.model.element.Modifier.STATIC;
import static net.zerobuilder.AccessLevel.UNSPECIFIED;
import static net.zerobuilder.compiler.Messages.ErrorMessages.NO_TYPE_PARAMS_HERE;
import static net.zerobuilder.compiler.analyse.DtoGoalElement.ModuleChoice.BUILDER;
import static net.zerobuilder.compiler.analyse.DtoGoalElement.ModuleChoice.UPDATER;
import static net.zerobuilder.compiler.analyse.Utilities.downcase;
import static net.zerobuilder.compiler.analyse.Utilities.transform;
import static net.zerobuilder.compiler.common.LessTypes.asTypeElement;
import static net.zerobuilder.compiler.generate.ZeroUtil.simpleName;

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
    R staticMethod(RegularStaticGoalElement regular);
    R projectable(RegularProjectableGoalElement projectable);
  }

  interface AbstractRegularGoalElement extends AbstractGoalElement {
    <R> R accept(RegularGoalElementCases<R> cases);
  }

  static <R> Function<AbstractRegularGoalElement, R> asFunction(RegularGoalElementCases<R> cases) {
    return element -> element.accept(cases);
  }

  static <R> Function<AbstractRegularGoalElement, R> regularGoalElementCases(
      Function<? super RegularGoalElement, ? extends R> generalFunction,
      Function<? super RegularStaticGoalElement, ? extends R> staticFunction,
      Function<? super RegularProjectableGoalElement, ? extends R> projectableFunction) {
    return asFunction(new RegularGoalElementCases<R>() {
      @Override
      public R general(RegularGoalElement regular) {
        return generalFunction.apply(regular);
      }
      @Override
      public R staticMethod(RegularStaticGoalElement regularStatic) {
        return staticFunction.apply(regularStatic);
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
          regularStatic -> regularStatic.executableElement,
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

  static final Function<AbstractGoalElement, Goal> goalAnnotation =
      goalElementCases(
          regularGoalElementCases(
              general -> general.goalAnnotation,
              regularStatic -> regularStatic.goalAnnotation,
              projectable -> projectable.goalAnnotation),
          bean -> bean.goalAnnotation);

  private static final Function<AbstractRegularGoalElement, AbstractGoalDetails> abstractDetails =
      regularGoalElementCases(
          general -> general.details,
          regularStatic -> regularStatic.details,
          projectable -> projectable.details);

  static final Function<AbstractGoalElement, String> goalName =
      goalElementCases(
          regular -> abstractDetails.apply(regular).name(),
          bean -> bean.details.name());

  static final Function<AbstractGoalElement, Element> element =
      goalElementCases(
          regularGoalElementCases(
              regular -> regular.executableElement,
              regularStatic -> regularStatic.executableElement,
              projected -> projected.executableElement),
          bean -> bean.beanType);

  private static final Function<AbstractGoalElement, Boolean> fnIsRegular =
      goalElementCases(regular -> true, bean -> false);

  static final Predicate<AbstractGoalElement> isRegular =
      el -> fnIsRegular.apply(el);

  static final class RegularGoalElement implements AbstractRegularGoalElement {
    final AbstractRegularDetails details;
    final ExecutableElement executableElement;
    final Goal goalAnnotation;

    private RegularGoalElement(ExecutableElement element, AbstractRegularDetails details) {
      this.goalAnnotation = element.getAnnotation(Goal.class);
      this.details = details;
      this.executableElement = element;
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

  static final class RegularStaticGoalElement implements AbstractRegularGoalElement {
    final StaticMethodGoalDetails details;
    final ExecutableElement executableElement;
    final Goal goalAnnotation;

    private RegularStaticGoalElement(ExecutableElement element, StaticMethodGoalDetails details) {
      this.goalAnnotation = element.getAnnotation(Goal.class);
      this.details = details;
      this.executableElement = element;
    }

    @Override
    public <R> R accept(GoalElementCases<R> goalElementCases) {
      return goalElementCases.regularGoal(this);
    }

    @Override
    public <R> R accept(RegularGoalElementCases<R> cases) {
      return cases.staticMethod(this);
    }
  }

  static final class RegularProjectableGoalElement implements AbstractRegularGoalElement {
    final ProjectableDetails details;
    final ExecutableElement executableElement;
    final Goal goalAnnotation;

    private RegularProjectableGoalElement(ExecutableElement element, ProjectableDetails details) {
      this.goalAnnotation = element.getAnnotation(Goal.class);
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
    final Goal goalAnnotation;
    final ModuleChoice moduleChoice;

    private BeanGoalElement(ClassName goalType, String name, TypeElement beanType,
                            Goal goalAnnotation, Access access, ModuleChoice moduleChoice) {
      this.goalAnnotation = goalAnnotation;
      this.moduleChoice = moduleChoice;
      this.details = new BeanGoalDetails(goalType, name, access);
      this.beanType = beanType;
    }

    static List<BeanGoalElement> create(TypeElement beanType, AccessLevel defaultAccess) {
      ClassName goalType = ClassName.get(beanType);
      Goal goalAnnotation = beanType.getAnnotation(Goal.class);
      String name = goalName(goalAnnotation, goalType);
      List<ModuledOption> goalOptions = goalOptions(goalAnnotation, defaultAccess);
      return transform(goalOptions,
          goalOption -> new BeanGoalElement(goalType, name, beanType, goalAnnotation,
              goalOption.access, goalOption.module));
    }

    @Override
    public <R> R accept(GoalElementCases<R> goalElementCases) {
      return goalElementCases.beanGoal(this);
    }
  }

  static String goalName(Goal goalAnnotation, TypeName goalType) {
    return goalAnnotation.name().isEmpty()
        ? downcase(simpleName(goalType))
        : goalAnnotation.name();
  }

  private static Access accessLevelOverride(AccessLevel override, AccessLevel defaultAccess) {
    defaultAccess = defaultAccess == UNSPECIFIED
        ? AccessLevel.PUBLIC
        : defaultAccess;
    return override == UNSPECIFIED
        ? defaultAccess.access()
        : override.access();
  }

  enum ModuleChoice {
    UPDATER, BUILDER
  }

  static final class ModuledOption {
    final Access access;
    final ModuleChoice module;
    ModuledOption(Access access, ModuleChoice module) {
      this.access = access;
      this.module = module;
    }
    static ModuledOption create(Access access, ModuleChoice module) {
      return new ModuledOption(access, module);
    }
  }

  private static List<ModuledOption> goalOptions(Goal goalAnnotation, AccessLevel defaultAccess) {
    List<ModuledOption> options = new ArrayList<>(2);
    if (goalAnnotation.updater()) {
      options.add(ModuledOption.create(
          accessLevelOverride(goalAnnotation.updaterAccess(), defaultAccess), UPDATER));
    }
    if (goalAnnotation.builder()) {
      options.add(ModuledOption.create(
          accessLevelOverride(goalAnnotation.builderAccess(), defaultAccess), BUILDER));
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

  static List<AbstractRegularGoalElement> createRegular(ExecutableElement element, AccessLevel defaultAccess) {
    TypeName goalType = goalType(element);
    Goal goalAnnotation = element.getAnnotation(Goal.class);
    String name = goalName(goalAnnotation, goalType);
    List<ModuledOption> goalOptions = goalOptions(goalAnnotation, defaultAccess);
    String methodName = element.getSimpleName().toString();
    return transform(goalOptions,
        goalOption ->
            goalOption.module == BUILDER ?
                createBuilderGoal(element, goalType, name, methodName,
                    parameterNames(element), goalOption) :
                createUpdaterGoal(element, goalType, name, methodName,
                    parameterNames(element), goalOption));
  }

  private static AbstractRegularGoalElement createUpdaterGoal(ExecutableElement element, TypeName goalType, String name,
                                                              String methodName,
                                                              List<String> parameterNames, ModuledOption goalOption) {
    if (!element.getTypeParameters().isEmpty()) {
      throw new ValidationException(NO_TYPE_PARAMS_HERE, element);
    }
    ProjectableDetails details =
        element.getKind() == CONSTRUCTOR ?
            ConstructorGoalDetails.create(ClassName.get(asTypeElement(element.getEnclosingElement().asType())),
                name, parameterNames, goalOption.access) :
            StaticMethodGoalDetails.create(goalType, name, parameterNames, methodName, goalOption.access,
                emptyList(), emptyList(),
                element.getModifiers().contains(STATIC) ?
                    DetailsType.STATIC :
                    DetailsType.INSTANCE);
    return new RegularProjectableGoalElement(element, details);
  }

  private static AbstractRegularGoalElement createBuilderGoal(ExecutableElement element, TypeName goalType, String name,
                                                              String methodName,
                                                              List<String> parameterNames, ModuledOption goalOption) {
    List<TypeVariableName> typeParameters = element.getTypeParameters().stream()
        .map(TypeVariableName::get)
        .collect(toList());
    boolean isStatic = element.getModifiers().contains(STATIC);
    List<TypeVariableName> parentParameters = asTypeElement(element.getEnclosingElement().asType()).getTypeParameters()
        .stream().map(TypeVariableName::get).collect(toList());
    if (!element.getTypeParameters().isEmpty()
        || (element.getKind() == CONSTRUCTOR && !parentParameters.isEmpty())) {
      StaticMethodGoalDetails details = StaticMethodGoalDetails.create(goalType,
          name, parameterNames, methodName, goalOption.access, typeParameters,
          isStatic ?
              emptyList() :
              parentParameters,
          isStatic ?
              DetailsType.STATIC :
              element.getKind() == CONSTRUCTOR ?
                  DetailsType.CONSTRUCTOR :
                  DetailsType.INSTANCE);
      return new RegularStaticGoalElement(element, details);
    }
    if (element.getKind() == CONSTRUCTOR) {
      ConstructorGoalDetails details = ConstructorGoalDetails.create(
          ClassName.get(asTypeElement(element.getEnclosingElement().asType())),
          name, parameterNames, goalOption.access);
      return new RegularGoalElement(element, details);
    }
    AbstractRegularDetails details =
        isStatic ?
            StaticMethodGoalDetails.create(goalType, name, parameterNames, methodName, goalOption.access,
                typeParameters,
                emptyList(),
                DetailsType.STATIC) :
            InstanceMethodGoalDetails.create(goalType, name, parameterNames, methodName, goalOption.access);
    return new RegularGoalElement(element, details);
  }

  private DtoGoalElement() {
    throw new UnsupportedOperationException("no instances");
  }
}
