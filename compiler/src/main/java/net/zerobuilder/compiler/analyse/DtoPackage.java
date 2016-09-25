package net.zerobuilder.compiler.analyse;

import com.google.common.base.Function;
import com.squareup.javapoet.ClassName;
import com.squareup.javapoet.TypeName;
import net.zerobuilder.Goal;
import net.zerobuilder.compiler.analyse.DtoShared.BeanGoal;
import net.zerobuilder.compiler.analyse.DtoShared.RegularGoal;
import net.zerobuilder.compiler.analyse.GoalContextFactory.GoalKind;

import javax.lang.model.element.Element;
import javax.lang.model.element.ExecutableElement;
import javax.lang.model.element.TypeElement;
import javax.lang.model.util.Elements;

import static com.google.common.base.Strings.isNullOrEmpty;
import static javax.lang.model.element.ElementKind.CONSTRUCTOR;
import static javax.lang.model.element.Modifier.STATIC;
import static net.zerobuilder.compiler.Utilities.downcase;
import static net.zerobuilder.compiler.analyse.GoalContextFactory.GoalKind.INSTANCE_METHOD;
import static net.zerobuilder.compiler.analyse.GoalContextFactory.GoalKind.STATIC_METHOD;

final class DtoPackage {

  static final class GoalTypes {

    interface GoalElementCases<R> {
      R regularGoal(RegularGoalElement goal);
      R beanGoal(BeanGoalElement goal);
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

    static final GoalElementCases<String> getName = new GoalElementCases<String>() {
      @Override
      public String regularGoal(RegularGoalElement goal) {
        return goal.goal.name;
      }
      @Override
      public String beanGoal(BeanGoalElement goal) {
        return goal.goal.name;
      }
    };

    static final class RegularGoalElement extends AbstractGoalElement {
      final RegularGoal goal;
      final ExecutableElement executableElement;
      RegularGoalElement(ExecutableElement element, GoalKind kind, TypeName goalType, String name,
                         Elements elements) {
        super(element.getAnnotation(Goal.class), elements);
        this.goal = new RegularGoal(goalType, name, kind);
        this.executableElement = element;
      }
      static RegularGoalElement create(ExecutableElement element, Elements elements) {
        TypeName goalType = goalType(element);
        String name = goalName(element.getAnnotation(Goal.class), goalType);
        return new RegularGoalElement(element,
            element.getKind() == CONSTRUCTOR
                ? GoalKind.CONSTRUCTOR
                : element.getModifiers().contains(STATIC) ? STATIC_METHOD : INSTANCE_METHOD,
            goalType(element), name, elements);

      }
      <R> R accept(GoalElementCases<R> goalElementCases) {
        return goalElementCases.regularGoal(this);
      }
    }

    static final class BeanGoalElement extends AbstractGoalElement {
      final BeanGoal goal;
      final TypeElement beanTypeElement;
      private BeanGoalElement(Element field, ClassName goalType, String name, TypeElement beanTypeElement, Elements elements) {
        super(field.getAnnotation(Goal.class), elements);
        this.goal = new BeanGoal(goalType, name);
        this.beanTypeElement = beanTypeElement;
      }
      static BeanGoalElement create(TypeElement beanType, Elements elements) {
        ClassName goalType = ClassName.get(beanType);
        String name = goalName(beanType.getAnnotation(Goal.class), goalType);
        return new BeanGoalElement(beanType, goalType, name, beanType, elements);
      }
      <R> R accept(GoalElementCases<R> goalElementCases) {
        return goalElementCases.beanGoal(this);
      }
    }

    static final GoalTypes.GoalElementCases<Element> getElement = new GoalTypes.GoalElementCases<Element>() {
      @Override
      public Element regularGoal(RegularGoalElement executableGoal) {
        return executableGoal.executableElement;
      }
      @Override
      public Element beanGoal(BeanGoalElement beanGoal) {
        return beanGoal.beanTypeElement;
      }
    };

    private GoalTypes() {
      throw new UnsupportedOperationException("no instances");
    }
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
