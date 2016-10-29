package net.zerobuilder.compiler.analyse;

import com.squareup.javapoet.ClassName;
import com.squareup.javapoet.TypeName;
import net.zerobuilder.AccessLevel;
import net.zerobuilder.Goal;
import net.zerobuilder.compiler.generate.Access;
import net.zerobuilder.compiler.generate.DtoGoalDetails;
import net.zerobuilder.compiler.generate.DtoGoalDetails.AbstractGoalDetails;
import net.zerobuilder.compiler.generate.DtoGoalDetails.AbstractRegularDetails;
import net.zerobuilder.compiler.generate.DtoGoalDetails.BeanGoalDetails;
import net.zerobuilder.compiler.generate.DtoGoalDetails.ConstructorGoalDetails;
import net.zerobuilder.compiler.generate.DtoGoalDetails.GoalMethodType;
import net.zerobuilder.compiler.generate.DtoGoalDetails.MethodGoalDetails;
import net.zerobuilder.compiler.generate.DtoGoalDetails.ProjectableDetails;
import net.zerobuilder.compiler.generate.DtoGoalDetails.StaticMethodGoalDetails;

import javax.lang.model.element.ExecutableElement;
import javax.lang.model.element.TypeElement;
import javax.lang.model.element.VariableElement;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Function;

import static java.util.stream.Collectors.toList;
import static javax.lang.model.element.ElementKind.CONSTRUCTOR;
import static javax.lang.model.element.ElementKind.METHOD;
import static javax.lang.model.element.Modifier.STATIC;
import static net.zerobuilder.AccessLevel.UNSPECIFIED;
import static net.zerobuilder.compiler.Messages.ErrorMessages.NON_STATIC_UPDATER;
import static net.zerobuilder.compiler.analyse.Analyser.modules;
import static net.zerobuilder.compiler.analyse.Utilities.downcase;

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

  static <R> Function<AbstractRegularGoalElement, R> asFunction(RegularGoalElementCases<R> cases) {
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


  interface AbstractProjectableGoalElement extends AbstractGoalElement {
    <R> R accept(AbstractProjectableGoalElementCases<R> cases);
  }

  interface AbstractProjectableGoalElementCases<R> {
    R regular(RegularProjectableGoalElement regular);
    R bean(BeanGoalElement bean);
  }

  static <R> Function<AbstractProjectableGoalElement, R> asFunction(AbstractProjectableGoalElementCases<R> cases) {
    return element -> element.accept(cases);
  }

  static <R> Function<AbstractProjectableGoalElement, R> abstractProjectableGoalElementCases(
      Function<? super RegularProjectableGoalElement, ? extends R> regularFunction,
      Function<? super BeanGoalElement, ? extends R> beanFunction) {
    return asFunction(new AbstractProjectableGoalElementCases<R>() {
      @Override
      public R regular(RegularProjectableGoalElement regular) {
        return regularFunction.apply(regular);
      }
      @Override
      public R bean(BeanGoalElement bean) {
        return beanFunction.apply(bean);
      }
    });
  }

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
              projectable -> projectable.goalAnnotation),
          bean -> bean.goalAnnotation);

  static final Function<AbstractGoalElement, ModuleChoice> module =
      goalElementCases(
          regularGoalElementCases(
              general -> general.module,
              projectable -> projectable.module),
          bean -> bean.module);

  static final Function<AbstractGoalElement, String> goalName = asFunction(new GoalElementCases<String>() {
    @Override
    public String regularGoal(AbstractRegularGoalElement goal) {
      return abstractDetails.apply(goal).name();
    }
    @Override
    public String beanGoal(BeanGoalElement goal) {
      return goal.details.name();
    }
  });

  static final Function<AbstractRegularGoalElement, AbstractGoalDetails> abstractDetails =
      regularGoalElementCases(
          general -> general.details,
          projectable -> projectable.details);


  static final class RegularGoalElement implements AbstractRegularGoalElement {
    final List<ModuleChoice> modules;
    final AbstractRegularDetails details;
    final ExecutableElement executableElement;
    final Goal goalAnnotation;
    final ModuleChoice module;

    private RegularGoalElement(List<ModuleChoice> modules, ExecutableElement element, AbstractRegularDetails details, ModuleChoice module) {
      this.goalAnnotation = element.getAnnotation(Goal.class);
      this.module = module;
      this.modules = modules;
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

  static final class RegularProjectableGoalElement implements AbstractRegularGoalElement,
      AbstractProjectableGoalElement {
    final List<ModuleChoice> modules;
    final ProjectableDetails details;
    final ExecutableElement executableElement;
    final Goal goalAnnotation;
    final ModuleChoice module;

    private RegularProjectableGoalElement(List<ModuleChoice> modules, ExecutableElement element, ProjectableDetails details, ModuleChoice module) {
      this.goalAnnotation = element.getAnnotation(Goal.class);
      this.module = module;
      this.modules = modules;
      this.details = details;
      this.executableElement = element;
    }

    @Override
    public <R> R accept(GoalElementCases<R> goalElementCases) {
      return goalElementCases.regularGoal(this);
    }

    @Override
    public <R> R accept(AbstractProjectableGoalElementCases<R> cases) {
      return cases.regular(this);
    }

    @Override
    public <R> R accept(RegularGoalElementCases<R> cases) {
      return cases.projectable(this);
    }
  }

  private static List<String> parameterNames(ExecutableElement element) {
    List<String> builder = new ArrayList<>();
    for (VariableElement parameter : element.getParameters()) {
      builder.add(parameter.getSimpleName().toString());
    }
    return builder;
  }

  static final class BeanGoalElement implements AbstractProjectableGoalElement {
    final BeanGoalDetails details;
    final TypeElement beanType;
    final Goal goalAnnotation;
    final ModuleChoice module;

    private BeanGoalElement(ClassName goalType, String name, TypeElement beanType,
                            Goal goalAnnotation, Access access, ModuleChoice module) {
      this.goalAnnotation = goalAnnotation;
      this.module = module;
      this.details = new BeanGoalDetails(goalType, name, access);
      this.beanType = beanType;
    }

    static List<BeanGoalElement> create(TypeElement beanType, AccessLevel defaultAccess) {
      ClassName goalType = ClassName.get(beanType);
      Goal goalAnnotation = beanType.getAnnotation(Goal.class);
      String name = goalName(goalAnnotation, goalType);
      List<ModuledOption> goalOptions = goalOptions(goalAnnotation, defaultAccess);
      return goalOptions.stream()
          .map(goalOption ->
              new BeanGoalElement(goalType, name, beanType, goalAnnotation, goalOption.access, goalOption.module))
          .collect(toList());
    }

    @Override
    public <R> R accept(GoalElementCases<R> goalElementCases) {
      return goalElementCases.beanGoal(this);
    }

    @Override
    public <R> R accept(AbstractProjectableGoalElementCases<R> cases) {
      return cases.bean(this);
    }
  }

  static String goalName(Goal goalAnnotation, TypeName goalType) {
    return goalAnnotation.name().isEmpty()
        ? downcase(((ClassName) goalType.box()).simpleName())
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
          accessLevelOverride(goalAnnotation.updaterAccess(), defaultAccess), ModuleChoice.UPDATER));
    }
    if (goalAnnotation.builder()) {
      options.add(ModuledOption.create(
          accessLevelOverride(goalAnnotation.builderAccess(), defaultAccess), ModuleChoice.BUILDER));
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
    GoalMethodType goalMethodType = element.getModifiers().contains(STATIC)
        ? GoalMethodType.STATIC_METHOD
        : GoalMethodType.INSTANCE_METHOD;
    List<ModuleChoice> modules = modules(goalAnnotation);
    List<String> parameterNames = parameterNames(element);
    return goalOptions.stream()
        .map(goalOption -> {
          if (goalOption.module == ModuleChoice.BUILDER) {
            return new RegularGoalElement(modules, element,
                element.getKind() == CONSTRUCTOR ?
                    ConstructorGoalDetails.create(goalType, name, parameterNames, goalOption.access) :
                    element.getModifiers().contains(STATIC) ?
                        StaticMethodGoalDetails.create(goalType, name, parameterNames, methodName, goalOption.access) :
                        MethodGoalDetails.create(goalType, name, parameterNames, methodName, goalMethodType, goalOption.access),
                goalOption.module);
          } else {
            if (element.getKind() == METHOD && !element.getModifiers().contains(STATIC)) {
              throw new ValidationException(NON_STATIC_UPDATER, element);
            }
            return new RegularProjectableGoalElement(modules, element,
                element.getKind() == CONSTRUCTOR ?
                    ConstructorGoalDetails.create(goalType, name, parameterNames, goalOption.access) :
                    StaticMethodGoalDetails.create(goalType, name, parameterNames, methodName, goalOption.access),
                goalOption.module);
          }
        })
        .collect(toList());
  }

  private DtoGoalElement() {
    throw new UnsupportedOperationException("no instances");
  }
}
