package net.zerobuilder.compiler.analyse;

import com.google.common.base.Function;
import com.squareup.javapoet.ClassName;
import com.squareup.javapoet.TypeName;
import net.zerobuilder.Goal;

import javax.lang.model.element.Element;
import javax.lang.model.element.ExecutableElement;
import javax.lang.model.element.TypeElement;
import javax.lang.model.util.Elements;

import static com.google.common.base.Preconditions.checkNotNull;
import static com.google.common.base.Strings.isNullOrEmpty;
import static javax.lang.model.element.ElementKind.CONSTRUCTOR;
import static javax.lang.model.element.Modifier.STATIC;
import static net.zerobuilder.compiler.Utilities.downcase;
import static net.zerobuilder.compiler.analyse.GoalContextFactory.GoalKind.INSTANCE_METHOD;
import static net.zerobuilder.compiler.analyse.GoalContextFactory.GoalKind.STATIC_METHOD;

final class DtoPackage {

  static final class GoalTypes {

    interface GoalElementCases<R> {
      R executableGoal(ExecutableGoal executableGoal);
      R beanGoal(BeanGoal beanGoal);
    }

    static abstract class AbstractGoalElement {
      abstract <R> R accept(GoalElementCases<R> goalElementCases);
    }

    static <R> GoalElementCases<R> goalElementCases(
        final Function<ExecutableGoal, R> executableGoalFunction,
        final Function<BeanGoal, R> beanGoalFunction) {
      return new GoalElementCases<R>() {
        @Override
        public R executableGoal(ExecutableGoal executableGoal) {
          return executableGoalFunction.apply(executableGoal);
        }
        @Override
        public R beanGoal(BeanGoal beanGoal) {
          return beanGoalFunction.apply(beanGoal);
        }
      };
    }

    static abstract class GoalElement extends AbstractGoalElement {
      final Goal goalAnnotation;
      final String name;
      final Elements elements;
      GoalElement(Goal goalAnnotation, String name, Elements elements) {
        this.goalAnnotation = checkNotNull(goalAnnotation, "goalAnnotation");
        this.name = checkNotNull(name, "name");
        this.elements = elements;
      }
    }

    static final class ExecutableGoal extends GoalElement {
      final GoalContextFactory.GoalKind kind;
      final TypeName goalType;
      final ExecutableElement executableElement;
      ExecutableGoal(ExecutableElement element, GoalContextFactory.GoalKind kind, TypeName goalType, String name,
                     Elements elements) {
        super(element.getAnnotation(Goal.class), name, elements);
        this.goalType = goalType;
        this.kind = kind;
        this.executableElement = element;
      }
      static GoalElement create(ExecutableElement element, Elements elements) {
        TypeName goalType = goalType(element);
        String name = goalName(element.getAnnotation(Goal.class), goalType);
        return new ExecutableGoal(element,
            element.getKind() == CONSTRUCTOR
                ? GoalContextFactory.GoalKind.CONSTRUCTOR
                : element.getModifiers().contains(STATIC) ? STATIC_METHOD : INSTANCE_METHOD,
            goalType(element), name, elements);

      }
      <R> R accept(GoalElementCases<R> goalElementCases) {
        return goalElementCases.executableGoal(this);
      }
    }

    static final class BeanGoal extends GoalElement {
      final ClassName goalType;
      final TypeElement beanTypeElement;
      private BeanGoal(Element field, ClassName goalType, String name, TypeElement beanTypeElement, Elements elements) {
        super(field.getAnnotation(Goal.class), name, elements);
        this.goalType = goalType;
        this.beanTypeElement = beanTypeElement;
      }
      static GoalElement create(TypeElement beanType, Elements elements) {
        ClassName goalType = ClassName.get(beanType);
        String name = goalName(beanType.getAnnotation(Goal.class), goalType);
        return new BeanGoal(beanType, goalType, name, beanType, elements);
      }
      <R> R accept(GoalElementCases<R> goalElementCases) {
        return goalElementCases.beanGoal(this);
      }
    }

    private GoalTypes() {
      throw new UnsupportedOperationException("no instances");
    }

    static final GoalTypes.GoalElementCases<Element> getElement = new GoalTypes.GoalElementCases<Element>() {
      @Override
      public Element executableGoal(GoalTypes.ExecutableGoal executableGoal) {
        return executableGoal.executableElement;
      }
      @Override
      public Element beanGoal(GoalTypes.BeanGoal beanGoal) {
        return beanGoal.beanTypeElement;
      }
    };

  }

  private static String goalName(Goal goalAnnotation, TypeName goalType) {
    return isNullOrEmpty(goalAnnotation.name())
        ? downcase(((ClassName) goalType.box()).simpleName())
        : goalAnnotation.name();
  }

  private static TypeName goalType(ExecutableElement goal) {
    switch (goal.getKind()) {
      case CONSTRUCTOR:
        return ClassName.get(goal.getEnclosingElement().asType());
      default:
        return TypeName.get(goal.getReturnType());
    }
  }

  private DtoPackage() {
    throw new UnsupportedOperationException("no instances");
  }
}
