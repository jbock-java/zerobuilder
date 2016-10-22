package net.zerobuilder.compiler.generate;

import net.zerobuilder.compiler.generate.DtoBeanParameter.AbstractBeanParameter;
import net.zerobuilder.compiler.generate.DtoRegularParameter.ProjectedParameter;

import java.util.function.Function;

public final class DtoProjectedParameter {

  interface AbstractProjectedParameter {
    <R> R acceptProjected(ProjectedParameterCases<R> cases);
  }

  interface ProjectedParameterCases<R> {
    R projectedRegular(ProjectedParameter regular);
    R projectedBean(AbstractBeanParameter bean);
  }

  static <R> Function<AbstractProjectedParameter, R> asFunction(ProjectedParameterCases<R> cases) {
    return parameter -> parameter.acceptProjected(cases);
  }

  static <R> Function<AbstractProjectedParameter, R> projectedParameterCases(
      Function<ProjectedParameter, R> regularFunction,
      Function<AbstractBeanParameter, R> beanFunction) {
    return asFunction(new ProjectedParameterCases<R>() {
      @Override
      public R projectedRegular(ProjectedParameter regular) {
        return regularFunction.apply(regular);
      }
      @Override
      public R projectedBean(AbstractBeanParameter bean) {
        return beanFunction.apply(bean);
      }
    });
  }

  private DtoProjectedParameter() {
    throw new UnsupportedOperationException("no instances");
  }
}
