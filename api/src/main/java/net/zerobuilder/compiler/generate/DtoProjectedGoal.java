package net.zerobuilder.compiler.generate;

import com.squareup.javapoet.TypeVariableName;
import net.zerobuilder.compiler.generate.DtoBeanGoal.BeanGoalContext;
import net.zerobuilder.compiler.generate.DtoGoalContext.AbstractGoalContext;
import net.zerobuilder.compiler.generate.DtoProjectedRegularGoalContext.ProjectedRegularGoalContext;

import java.util.List;
import java.util.function.Function;

import static java.util.Collections.emptyList;
import static java.util.function.Function.identity;

public final class DtoProjectedGoal {

  public interface ProjectedGoal {
    <R> R acceptProjected(ProjectedGoalCases<R> cases);
  }

  interface ProjectedGoalCases<R> {
    R regular(ProjectedRegularGoalContext regular);
    R bean(BeanGoalContext bean);
  }

  public static <R> Function<ProjectedGoal, R> asFunction(ProjectedGoalCases<R> cases) {
    return goal -> goal.acceptProjected(cases);
  }

  public static <R> Function<ProjectedGoal, R> projectedGoalCases(
      Function<ProjectedRegularGoalContext, R> regularFunction,
      Function<BeanGoalContext, R> beanFunction) {
    return asFunction(new ProjectedGoalCases<R>() {
      @Override
      public R regular(ProjectedRegularGoalContext regular) {
        return regularFunction.apply(regular);
      }
      @Override
      public R bean(BeanGoalContext bean) {
        return beanFunction.apply(bean);
      }
    });
  }

  private static <R> Function<ProjectedGoal, R> restrict(Function<AbstractGoalContext, R> function) {
    return projectedGoalCases(
        regular -> function.apply(regular),
        bean -> function.apply(bean)
    );
  }

  public static final Function<ProjectedGoal, AbstractGoalContext> goalContext =
      restrict(identity());

  public static final Function<ProjectedGoal, List<TypeVariableName>> instanceTypeParameters =
      projectedGoalCases(
          DtoProjectedRegularGoalContext.instanceTypeParameters,
          bean -> emptyList());

  static final Function<ProjectedGoal, DtoContext.GoalContext> context =
      restrict(AbstractGoalContext::context);

  private DtoProjectedGoal() {
    throw new UnsupportedOperationException("no instances");
  }
}
