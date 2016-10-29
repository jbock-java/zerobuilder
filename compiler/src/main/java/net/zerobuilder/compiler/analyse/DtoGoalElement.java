package net.zerobuilder.compiler.analyse;

import com.squareup.javapoet.ClassName;
import com.squareup.javapoet.TypeName;
import net.zerobuilder.AccessLevel;
import net.zerobuilder.Goal;
import net.zerobuilder.compiler.generate.Access;
import net.zerobuilder.compiler.generate.DtoGoalDetails.AbstractRegularDetails;
import net.zerobuilder.compiler.generate.DtoGoalDetails.BeanGoalDetails;
import net.zerobuilder.compiler.generate.DtoGoalDetails.ConstructorGoalDetails;
import net.zerobuilder.compiler.generate.DtoGoalDetails.GoalMethodType;
import net.zerobuilder.compiler.generate.DtoGoalDetails.MethodGoalDetails;

import javax.lang.model.element.ExecutableElement;
import javax.lang.model.element.TypeElement;
import javax.lang.model.element.VariableElement;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Function;

import static java.util.stream.Collectors.toList;
import static javax.lang.model.element.ElementKind.CONSTRUCTOR;
import static javax.lang.model.element.Modifier.STATIC;
import static net.zerobuilder.AccessLevel.UNSPECIFIED;
import static net.zerobuilder.compiler.analyse.Analyser.modules;
import static net.zerobuilder.compiler.analyse.Utilities.downcase;

final class DtoGoalElement {

  interface GoalElementCases<R> {
    R regularGoal(RegularGoalElement goal);
    R beanGoal(BeanGoalElement goal);
  }

  private static <R> Function<AbstractGoalElement, R> asFunction(final GoalElementCases<R> cases) {
    return goal -> goal.accept(cases);
  }

  static abstract class AbstractGoalElement {
    final Goal goalAnnotation;
    final ModuleChoice module;
    AbstractGoalElement(Goal goalAnnotation, ModuleChoice module) {
      this.goalAnnotation = goalAnnotation;
      this.module = module;
    }
    abstract <R> R accept(GoalElementCases<R> goalElementCases);
  }

  static <R> Function<AbstractGoalElement, R> goalElementCases(
      final Function<RegularGoalElement, ? extends R> regularGoalFunction,
      final Function<BeanGoalElement, ? extends R> beanGoalFunction) {
    return asFunction(new GoalElementCases<R>() {
      @Override
      public R regularGoal(RegularGoalElement executableGoal) {
        return regularGoalFunction.apply(executableGoal);
      }
      @Override
      public R beanGoal(BeanGoalElement beanGoal) {
        return beanGoalFunction.apply(beanGoal);
      }
    });
  }

  static final Function<AbstractGoalElement, String> goalName = asFunction(new GoalElementCases<String>() {
    @Override
    public String regularGoal(RegularGoalElement goal) {
      return goal.details.name();
    }
    @Override
    public String beanGoal(BeanGoalElement goal) {
      return goal.details.name();
    }
  });

  static final class RegularGoalElement extends AbstractGoalElement {
    final List<ModuleChoice> modules;
    final AbstractRegularDetails details;
    final ExecutableElement executableElement;

    private RegularGoalElement(List<ModuleChoice> modules, ExecutableElement element, AbstractRegularDetails details, ModuleChoice module) {
      super(element.getAnnotation(Goal.class), module);
      this.modules = modules;
      this.details = details;
      this.executableElement = element;
    }

    static List<RegularGoalElement> create(ExecutableElement element, AccessLevel defaultAccess) {
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
          .map(goalOption ->
              new RegularGoalElement(modules, element,
                  element.getKind() == CONSTRUCTOR
                      ? ConstructorGoalDetails.create(goalType, name, parameterNames, goalOption.access)
                      : MethodGoalDetails.create(goalType, name, parameterNames, methodName, goalMethodType, goalOption.access),
                  goalOption.module))
          .collect(toList());
    }

    <R> R accept(GoalElementCases<R> goalElementCases) {
      return goalElementCases.regularGoal(this);
    }
  }

  private static List<String> parameterNames(ExecutableElement element) {
    List<String> builder = new ArrayList<>();
    for (VariableElement parameter : element.getParameters()) {
      builder.add(parameter.getSimpleName().toString());
    }
    return builder;
  }

  static final class BeanGoalElement extends AbstractGoalElement {
    final BeanGoalDetails details;
    final TypeElement beanType;

    private BeanGoalElement(ClassName goalType, String name, TypeElement beanType,
                            Goal goalAnnotation, Access access, ModuleChoice module) {
      super(goalAnnotation, module);
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
    <R> R accept(GoalElementCases<R> goalElementCases) {
      return goalElementCases.beanGoal(this);
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

  private DtoGoalElement() {
    throw new UnsupportedOperationException("no instances");
  }
}
