package net.zerobuilder.compiler.generate;

import net.zerobuilder.compiler.generate.DtoBeanGoal.BeanGoalContext;
import net.zerobuilder.compiler.generate.DtoGoalContext.AbstractGoalContext;
import net.zerobuilder.compiler.generate.DtoRegularGoal.SimpleRegularGoalContext;
import net.zerobuilder.compiler.generate.DtoStep.AbstractStep;

import java.util.List;
import java.util.function.Function;

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

  static <R> Function<SimpleGoal, R> restrict(Function<AbstractGoalContext, R> function) {
    return simpleGoalCases(
        regular -> function.apply(regular),
        bean -> function.apply(bean)
    );
  }

  public static final Function<SimpleGoal, DtoContext.BuildersContext> context =
      restrict(AbstractGoalContext::context);

  public static final Function<SimpleGoal, String> name =
      simpleGoalCases(
          simpleRegularGoalContext -> simpleRegularGoalContext.regularDetails().name,
          bean -> bean.details.name);

  public static final Function<SimpleGoal, List<? extends AbstractStep>> abstractSteps =
      restrict(AbstractGoalContext::steps);

  private DtoSimpleGoal() {
    throw new UnsupportedOperationException("no instances");
  }
}
