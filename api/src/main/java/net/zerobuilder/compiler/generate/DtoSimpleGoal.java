package net.zerobuilder.compiler.generate;

import net.zerobuilder.compiler.generate.DtoBeanGoal.BeanGoalContext;
import net.zerobuilder.compiler.generate.DtoRegularGoal.SimpleRegularGoalContext;
import net.zerobuilder.compiler.generate.DtoStep.AbstractStep;

import java.util.List;
import java.util.function.Function;

import static java.util.Collections.unmodifiableList;

public final class DtoSimpleGoal {

  public interface SimpleGoal {
    <R> R acceptSimple(SimpleGoalCases<R> cases);
  }

  interface SimpleGoalCases<R> {
    R regular(SimpleRegularGoalContext regular);
    R bean(BeanGoalContext bean);
  }

  static <R> Function<SimpleGoal, R> asFunction(SimpleGoalCases<R> cases) {
    return goal -> goal.acceptSimple(cases);
  }

  public static <R> Function<SimpleGoal, R> simpleGoalCases(
      Function<? super SimpleRegularGoalContext, ? extends R> regularFunction,
      Function<? super BeanGoalContext, ? extends R> beanFunction) {
    return asFunction(new SimpleGoalCases<R>() {
      @Override
      public R regular(SimpleRegularGoalContext regular) {
        return regularFunction.apply(regular);
      }
      @Override
      public R bean(BeanGoalContext bean) {
        return beanFunction.apply(bean);
      }
    });
  }

  public static final Function<SimpleGoal, DtoContext.GoalContext> context =
      simpleGoalCases(
          DtoRegularGoal.regularGoalContextCases(
              constructor -> constructor.context,
              method -> method.context,
              staticMethod -> staticMethod.context),
          bean -> bean.context);

  public static final Function<SimpleGoal, String> name =
      simpleGoalCases(
          DtoRegularGoal.regularGoalContextCases(
              constructor -> constructor.details.name,
              method -> method.details.name,
              staticMethod -> staticMethod.details.name),
          bean -> bean.details.name);

  public static final Function<SimpleGoal, List<? extends AbstractStep>> abstractSteps =
      simpleGoalCases(
          DtoRegularGoal.regularGoalContextCases(
              constructor -> constructor.steps,
              method -> method.steps,
              staticMethod -> staticMethod.steps),
          bean -> unmodifiableList(bean.steps));

  private DtoSimpleGoal() {
    throw new UnsupportedOperationException("no instances");
  }
}
