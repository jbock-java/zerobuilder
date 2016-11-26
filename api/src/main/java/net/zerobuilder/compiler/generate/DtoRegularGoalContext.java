package net.zerobuilder.compiler.generate;

import net.zerobuilder.compiler.generate.DtoGoalContext.AbstractGoalContext;
import net.zerobuilder.compiler.generate.DtoProjectedRegularGoalContext.ProjectedRegularGoalContext;
import net.zerobuilder.compiler.generate.DtoRegularGoal.SimpleRegularGoalContext;

import java.util.function.Function;

import static net.zerobuilder.compiler.generate.DtoProjectedRegularGoalContext.projectedRegularGoalContextCases;

final class DtoRegularGoalContext {

  public static abstract class RegularGoalContext extends AbstractGoalContext {

    public final Boolean mayReuse() {
      return mayReuse.apply(this);
    }

    public final DtoContext.GoalContext context() {
      return context.apply(this);
    }

    abstract <R> R acceptRegular(RegularGoalContextCases<R> cases);
    @Override
    final <R> R accept(DtoGoalContext.GoalCases<R> cases) {
      return cases.regularGoal(this);
    }
  }

  interface RegularGoalContextCases<R> {
    R simple(SimpleRegularGoalContext simple);
    R projected(ProjectedRegularGoalContext projected);
  }

  static <R> Function<RegularGoalContext, R> asFunction(RegularGoalContextCases<R> cases) {
    return goal -> goal.acceptRegular(cases);
  }

  static final Function<RegularGoalContext, Boolean> mayReuse =
      regularGoalContextCases(
          DtoRegularGoal.mayReuse,
          DtoProjectedRegularGoalContext.mayReuse);

  static <R> Function<RegularGoalContext, R> regularGoalContextCases(
      Function<? super SimpleRegularGoalContext, ? extends R> simpleFunction,
      Function<? super ProjectedRegularGoalContext, ? extends R> projectedFunction) {
    return asFunction(new RegularGoalContextCases<R>() {
      @Override
      public R simple(SimpleRegularGoalContext simple) {
        return simpleFunction.apply(simple);
      }
      @Override
      public R projected(ProjectedRegularGoalContext projected) {
        return projectedFunction.apply(projected);
      }
    });
  }

  private static final Function<RegularGoalContext, DtoContext.GoalContext> context =
      regularGoalContextCases(
          DtoRegularGoal.regularGoalContextCases(
              constructor -> constructor.context,
              method -> method.context,
              staticMethod -> staticMethod.context),
          projectedRegularGoalContextCases(
              method -> method.context,
              constructor -> constructor.context));

  private DtoRegularGoalContext() {
    throw new UnsupportedOperationException("no instances");
  }
}
