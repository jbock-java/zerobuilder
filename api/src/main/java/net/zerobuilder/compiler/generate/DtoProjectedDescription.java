package net.zerobuilder.compiler.generate;

import net.zerobuilder.compiler.generate.DtoBeanGoalDescription.BeanGoalDescription;
import net.zerobuilder.compiler.generate.DtoRegularGoalDescription.ProjectedRegularGoalDescription;

import java.util.function.Function;

public final class DtoProjectedDescription {

  public interface ProjectedDescription {
    <R> R acceptProjected(ProjectedDescriptionCases<R> cases);
  }

  interface ProjectedDescriptionCases<R> {
    R regular(ProjectedRegularGoalDescription regular);
    R bean(BeanGoalDescription bean);
  }

  static <R> Function<ProjectedDescription, R> asFunction(ProjectedDescriptionCases<R> cases) {
    return description -> description.acceptProjected(cases);
  }

  static <R> Function<ProjectedDescription, R> projectedDescriptionCases(
      Function<ProjectedRegularGoalDescription, R> regularFunction,
      Function<BeanGoalDescription, R> beanFunction) {
    return asFunction(new ProjectedDescriptionCases<R>() {
      @Override
      public R regular(ProjectedRegularGoalDescription regular) {
        return regularFunction.apply(regular);
      }
      @Override
      public R bean(BeanGoalDescription bean) {
        return beanFunction.apply(bean);
      }
    });
  }

  private DtoProjectedDescription() {
    throw new UnsupportedOperationException("no instances");
  }
}
