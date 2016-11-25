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

  private DtoProjectedParameter() {
    throw new UnsupportedOperationException("no instances");
  }
}
