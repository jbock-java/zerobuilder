package net.zerobuilder.compiler.generate;

import net.zerobuilder.compiler.generate.DtoBeanGoalDescription.BeanGoalDescription;
import net.zerobuilder.compiler.generate.DtoRegularGoalDescription.SimpleRegularGoalDescription;
import net.zerobuilder.compiler.generate.DtoRegularGoalDescription.SimpleStaticGoalDescription;

import java.util.function.Function;

public final class DtoSimpleDescription {

  public interface SimpleDescription {
    <R> R acceptSimple(SimpleDescriptionCases<R> cases);
  }

  interface SimpleDescriptionCases<R> {
    R regular(SimpleRegularGoalDescription regular);
    R regularStatic(SimpleStaticGoalDescription regular);
    R bean(BeanGoalDescription bean);
  }

  static <R> Function<SimpleDescription, R> asFunction(SimpleDescriptionCases<R> cases) {
    return description -> description.acceptSimple(cases);
  }

  static <R> Function<SimpleDescription, R> simpleDescriptionCases(
      Function<SimpleRegularGoalDescription, R> regularFunction,
      Function<SimpleStaticGoalDescription, R> staticFunction,
      Function<BeanGoalDescription, R> beanFunction) {
    return asFunction(new SimpleDescriptionCases<R>() {
      @Override
      public R regular(SimpleRegularGoalDescription regular) {
        return regularFunction.apply(regular);
      }
      @Override
      public R regularStatic(SimpleStaticGoalDescription regular) {
        return staticFunction.apply(regular);
      }
      @Override
      public R bean(BeanGoalDescription bean) {
        return beanFunction.apply(bean);
      }
    });
  }

  private DtoSimpleDescription() {
    throw new UnsupportedOperationException("no instances");
  }
}
