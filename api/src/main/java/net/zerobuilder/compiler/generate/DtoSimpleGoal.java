package net.zerobuilder.compiler.generate;

import com.squareup.javapoet.TypeName;
import net.zerobuilder.compiler.generate.DtoBeanGoal.BeanGoalContext;
import net.zerobuilder.compiler.generate.DtoGoalContext.AbstractGoalContext;
import net.zerobuilder.compiler.generate.DtoProjectedRegularGoalContext.ProjectedRegularGoalContext;
import net.zerobuilder.compiler.generate.DtoRegularGoal.AbstractRegularGoalContext;

import java.util.List;
import java.util.function.Function;

import static java.util.function.Function.identity;

public final class DtoSimpleGoal {

  interface SimpleGoal {
    <R> R acceptSimple(SimpleGoalCases<R> cases);
  }

  interface SimpleGoalCases<R> {
    R regular(AbstractRegularGoalContext regular);
    R bean(BeanGoalContext bean);
  }

  static <R> Function<SimpleGoal, R> asFunction(SimpleGoalCases<R> cases) {
    return goal -> goal.acceptSimple(cases);
  }

  static <R> Function<SimpleGoal, R> simpleGoalCases(
      Function<? super AbstractRegularGoalContext, ? extends R> regularFunction,
      Function<? super BeanGoalContext, ? extends R> beanFunction) {
    return asFunction(new SimpleGoalCases<R>() {
      @Override
      public R regular(AbstractRegularGoalContext regular) {
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

  static final Function<SimpleGoal, AbstractGoalContext> goalContext =
      restrict(identity());

  static final Function<SimpleGoal, TypeName> goalType =
      restrict(AbstractGoalContext::goalType);

  static final Function<SimpleGoal, DtoContext.BuildersContext> context =
      restrict(AbstractGoalContext::context);

  static final Function<SimpleGoal, String> name =
      restrict(AbstractGoalContext::name);

  static final Function<SimpleGoal, List<? extends DtoStep.AbstractStep>> abstractSteps =
      restrict(AbstractGoalContext::steps);

  private DtoSimpleGoal() {
    throw new UnsupportedOperationException("no instances");
  }
}
