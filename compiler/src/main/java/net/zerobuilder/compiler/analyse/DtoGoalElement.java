package net.zerobuilder.compiler.analyse;

import com.google.common.base.Function;
import com.google.common.collect.ImmutableList;
import com.squareup.javapoet.ClassName;
import com.squareup.javapoet.TypeName;
import net.zerobuilder.Goal;
import net.zerobuilder.compiler.analyse.DtoGoal.BeanGoal;
import net.zerobuilder.compiler.analyse.DtoGoal.ConstructorGoal;
import net.zerobuilder.compiler.analyse.DtoGoal.GoalOptions;
import net.zerobuilder.compiler.analyse.DtoGoal.MethodGoal;
import net.zerobuilder.compiler.analyse.DtoGoal.RegularGoal;

import javax.lang.model.element.Element;
import javax.lang.model.element.ExecutableElement;
import javax.lang.model.element.TypeElement;
import javax.lang.model.element.VariableElement;
import javax.lang.model.util.Elements;

import static com.google.common.base.Strings.isNullOrEmpty;
import static javax.lang.model.element.ElementKind.CONSTRUCTOR;
import static javax.lang.model.element.Modifier.STATIC;
import static net.zerobuilder.compiler.Utilities.downcase;

final class DtoGoalElement {

  interface GoalElementCases<R> {
    R regularGoal(RegularGoalElement goal);
    R beanGoal(BeanGoalElement goal);
  }

  static <R> Function<AbstractGoalElement, R> asFunction(final GoalElementCases<R> cases) {
    return new Function<AbstractGoalElement, R>() {
      @Override
      public R apply(AbstractGoalElement goal) {
        return goal.accept(cases);
      }
    };
  }

  static abstract class AbstractGoalElement {
    final Goal goalAnnotation;
    final Elements elements;
    AbstractGoalElement(Goal goalAnnotation, Elements elements) {
      this.goalAnnotation = goalAnnotation;
      this.elements = elements;
    }
    abstract <R> R accept(GoalElementCases<R> goalElementCases);
  }

  static <R> GoalElementCases<R> goalElementCases(
      final Function<RegularGoalElement, R> regularGoalFunction,
      final Function<BeanGoalElement, R> beanGoalFunction) {
    return new GoalElementCases<R>() {
      @Override
      public R regularGoal(RegularGoalElement executableGoal) {
        return regularGoalFunction.apply(executableGoal);
      }
      @Override
      public R beanGoal(BeanGoalElement beanGoal) {
        return beanGoalFunction.apply(beanGoal);
      }
    };
  }

  static final Function<AbstractGoalElement, String> goalName = asFunction(new GoalElementCases<String>() {
    @Override
    public String regularGoal(RegularGoalElement goal) {
      return goal.goal.name;
    }
    @Override
    public String beanGoal(BeanGoalElement goal) {
      return goal.goal.name;
    }
  });

  static final class RegularGoalElement extends AbstractGoalElement {
    final RegularGoal goal;
    final ExecutableElement executableElement;

    private RegularGoalElement(ExecutableElement element, RegularGoal goal, Elements elements) {
      super(element.getAnnotation(Goal.class), elements);
      this.goal = goal;
      this.executableElement = element;
    }

    static RegularGoalElement create(ExecutableElement element, Elements elements) {
      TypeName goalType = goalType(element);
      Goal goalAnnotation = element.getAnnotation(Goal.class);
      String name = goalName(goalAnnotation, goalType);
      GoalOptions goalOptions = goalOptions(goalAnnotation);
      String methodName = element.getSimpleName().toString();
      boolean instance = !element.getModifiers().contains(STATIC);
      ImmutableList<String> parameterNames = parameterNames(element);
      RegularGoal goal = element.getKind() == CONSTRUCTOR
          ? new ConstructorGoal(goalType, name, parameterNames, goalOptions)
          : new MethodGoal(goalType, name, parameterNames, methodName, instance, goalOptions);
      return new RegularGoalElement(element, goal, elements);
    }

    <R> R accept(GoalElementCases<R> goalElementCases) {
      return goalElementCases.regularGoal(this);
    }
  }

  private static ImmutableList<String> parameterNames(ExecutableElement element) {
    ImmutableList.Builder<String> builder = ImmutableList.builder();
    for (VariableElement parameter : element.getParameters()) {
      builder.add(parameter.getSimpleName().toString());
    }
    return builder.build();
  }

  static final class BeanGoalElement extends AbstractGoalElement {
    final BeanGoal goal;
    final TypeElement beanType;
    private BeanGoalElement(ClassName goalType, String name, TypeElement beanType, Elements elements,
                            Goal goalAnnotation) {
      super(goalAnnotation, elements);
      this.goal = new BeanGoal(goalType, name, goalOptions(goalAnnotation));
      this.beanType = beanType;
    }
    static BeanGoalElement create(TypeElement beanType, Elements elements) {
      ClassName goalType = ClassName.get(beanType);
      Goal goalAnnotation = beanType.getAnnotation(Goal.class);
      String name = goalName(goalAnnotation, goalType);
      return new BeanGoalElement(goalType, name, beanType, elements, goalAnnotation);
    }
    <R> R accept(GoalElementCases<R> goalElementCases) {
      return goalElementCases.beanGoal(this);
    }
  }

  static final GoalElementCases<Element> getElement = new GoalElementCases<Element>() {
    @Override
    public Element regularGoal(RegularGoalElement executableGoal) {
      return executableGoal.executableElement;
    }
    @Override
    public Element beanGoal(BeanGoalElement beanGoal) {
      return beanGoal.beanType;
    }
  };

  private static String goalName(Goal goalAnnotation, TypeName goalType) {
    return isNullOrEmpty(goalAnnotation.name())
        ? downcase(((ClassName) goalType.box()).simpleName())
        : goalAnnotation.name();
  }

  private static GoalOptions goalOptions(Goal goalAnnotation) {
    return new GoalOptions(
        goalAnnotation.builderAccess(),
        goalAnnotation.toBuilderAccess());
  }

  private static TypeName goalType(ExecutableElement goal) {
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
