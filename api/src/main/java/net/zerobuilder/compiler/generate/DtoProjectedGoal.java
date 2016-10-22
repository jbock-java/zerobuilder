package net.zerobuilder.compiler.generate;

import net.zerobuilder.compiler.generate.DtoBeanGoal.BeanGoalContext;
import net.zerobuilder.compiler.generate.DtoConstructorGoal.ProjectedConstructorGoalContext;
import net.zerobuilder.compiler.generate.DtoMethodGoal.ProjectedMethodGoalContext;

import java.util.function.Function;

public final class DtoProjectedGoal {

  interface ProjectedGoal {
    <R> R acceptProjected(ProjectedGoalCases<R> cases);
  }

  interface ProjectedGoalCases<R> {
    R method(ProjectedMethodGoalContext method);
    R constructor(ProjectedConstructorGoalContext constructor);
    R bean(BeanGoalContext bean);
  }

  static <R> Function<ProjectedGoal, R> asFunction(ProjectedGoalCases<R> cases) {
    return goal -> goal.acceptProjected(cases);
  }

  static <R> Function<ProjectedGoal, R> projectedGoalCases(
      Function<ProjectedMethodGoalContext, R> methodFunction,
      Function<ProjectedConstructorGoalContext, R> constructorFunction,
      Function<BeanGoalContext, R> beanFunction) {
    return asFunction(new ProjectedGoalCases<R>() {
      @Override
      public R method(ProjectedMethodGoalContext method) {
        return methodFunction.apply(method);
      }
      @Override
      public R constructor(ProjectedConstructorGoalContext constructor) {
        return constructorFunction.apply(constructor);
      }
      @Override
      public R bean(BeanGoalContext bean) {
        return beanFunction.apply(bean);
      }
    });
  }

  private DtoProjectedGoal() {
    throw new UnsupportedOperationException("no instances");
  }
}
