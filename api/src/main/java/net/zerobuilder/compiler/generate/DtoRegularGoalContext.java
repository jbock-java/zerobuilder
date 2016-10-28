package net.zerobuilder.compiler.generate;

import com.squareup.javapoet.TypeName;
import net.zerobuilder.compiler.generate.DtoGoalContext.AbstractGoalContext;
import net.zerobuilder.compiler.generate.DtoProjectedRegularGoalContext.ProjectedRegularGoalContext;
import net.zerobuilder.compiler.generate.DtoRegularGoal.SimpleRegularGoalContext;

import java.util.List;
import java.util.function.Function;

final class DtoRegularGoalContext {

  static abstract class RegularGoalContext extends AbstractGoalContext {

    final List<TypeName> thrownTypes;

    RegularGoalContext(List<TypeName> thrownTypes) {
      this.thrownTypes = thrownTypes;
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

  static <R> Function<RegularGoalContext, R> regularGoalContextCases(
      Function<SimpleRegularGoalContext, R> simpleFunction,
      Function<ProjectedRegularGoalContext, R> projectedFunction) {
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


  private DtoRegularGoalContext() {
    throw new UnsupportedOperationException("no instances");
  }
}
