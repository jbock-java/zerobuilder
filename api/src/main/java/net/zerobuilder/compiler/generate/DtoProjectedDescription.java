package net.zerobuilder.compiler.generate;

import net.zerobuilder.compiler.generate.DtoBeanGoalDescription.BeanGoalDescription;
import net.zerobuilder.compiler.generate.DtoRegularGoalDescription.ProjectedRegularGoalDescription;

import java.util.function.Function;

public final class DtoProjectedDescription {

  interface ProjectedDescription {
    <R> R acceptProjected(ProjectedDescriptionCases<R> cases);
  }

  interface ProjectedDescriptionCases<R> {
    R bean(BeanGoalDescription bean);
    R regular(ProjectedRegularGoalDescription regular);
  }

  static <R> Function<ProjectedDescription, R> asFunction(ProjectedDescriptionCases<R> cases) {
    return description -> description.acceptProjected(cases);
  }

  static <R> Function<ProjectedDescription, R> projectedDescriptionCases(
    Function<BeanGoalDescription, R> beanFunction,
    Function<ProjectedRegularGoalDescription, R> regularFunction) {
    return asFunction(new ProjectedDescriptionCases<R>() {
      @Override
      public R bean(BeanGoalDescription bean) {
        return beanFunction.apply(bean);
      }
      @Override
      public R regular(ProjectedRegularGoalDescription regular) {
        return regularFunction.apply(regular);
      }
    });
  }

  private DtoProjectedDescription() {
    throw new UnsupportedOperationException("no instances");
  }
}
