package net.zerobuilder.compiler.generate;

import net.zerobuilder.compiler.generate.DtoBeanGoalDescription.BeanGoalDescription;
import net.zerobuilder.compiler.generate.DtoContext.GoalContext;
import net.zerobuilder.compiler.generate.DtoModule.BeanModule;
import net.zerobuilder.compiler.generate.DtoModule.ProjectedModule;
import net.zerobuilder.compiler.generate.DtoModule.RegularSimpleModule;
import net.zerobuilder.compiler.generate.DtoRegularGoalDescription.ProjectedRegularGoalDescription;
import net.zerobuilder.compiler.generate.DtoRegularGoalDescription.SimpleRegularGoalDescription;

import java.util.function.BiFunction;
import java.util.function.Function;

public final class DtoGeneratorInput {

  interface GoalInputCases<R, P> {
    R projected(ProjectedGoalInput projected, P p);
    R regularSimple(RegularSimpleGoalInput regularSimple, P p);
    R bean(BeanGoalInput bean, P p);
  }

  public static abstract class AbstractGoalInput {
    abstract <R, P> R accept(GoalInputCases<R, P> cases, P p);
  }

  private static <R, P> BiFunction<AbstractGoalInput, P, R> asFunction(GoalInputCases<R, P> cases) {
    return (input, p) -> input.accept(cases, p);
  }

  static <R, P> BiFunction<AbstractGoalInput, P, R> goalInputCases(
      BiFunction<ProjectedGoalInput, P, R> projectedFunction,
      BiFunction<RegularSimpleGoalInput, P, R> regularSimpleFunction,
      BiFunction<BeanGoalInput, P, R> beanFunction) {
    return asFunction(new GoalInputCases<R, P>() {
      @Override
      public R projected(ProjectedGoalInput projected, P p) {
        return projectedFunction.apply(projected, p);
      }
      @Override
      public R regularSimple(RegularSimpleGoalInput regularSimple, P p) {
        return regularSimpleFunction.apply(regularSimple, p);
      }
      @Override
      public R bean(BeanGoalInput bean, P p) {
        return beanFunction.apply(bean, p);
      }
    });
  }

  static <R> Function<AbstractGoalInput, R> goalInputCases(
      Function<ProjectedGoalInput, R> projectedFunction,
      Function<RegularSimpleGoalInput, R> regularSimpleFunction,
      Function<BeanGoalInput, R> beanFunction) {
    BiFunction<AbstractGoalInput, Void, R> biFunction = asFunction(new GoalInputCases<R, Void>() {
      @Override
      public R projected(ProjectedGoalInput projected, Void _null) {
        return projectedFunction.apply(projected);
      }
      @Override
      public R regularSimple(RegularSimpleGoalInput regularSimple, Void _null) {
        return regularSimpleFunction.apply(regularSimple);
      }
      @Override
      public R bean(BeanGoalInput bean, Void _null) {
        return beanFunction.apply(bean);
      }
    });
    return input -> biFunction.apply(input, null);
  }

  public static final class RegularSimpleGoalInput extends AbstractGoalInput {
    final RegularSimpleModule module;
    final SimpleRegularGoalDescription description;
    public RegularSimpleGoalInput(RegularSimpleModule module, SimpleRegularGoalDescription description) {
      this.module = module;
      this.description = description;
    }
    @Override
    <R, P> R accept(GoalInputCases<R, P> cases, P p) {
      return cases.regularSimple(this, p);
    }
  }

  public static final class BeanGoalInput extends AbstractGoalInput {
    final BeanModule module;
    final BeanGoalDescription description;
    public BeanGoalInput(BeanModule module, BeanGoalDescription description) {
      this.module = module;
      this.description = description;
    }
    @Override
    <R, P> R accept(GoalInputCases<R, P> cases, P p) {
      return cases.bean(this, p);
    }
  }

  public static final class ProjectedGoalInput extends AbstractGoalInput {
    final ProjectedModule module;
    final ProjectedRegularGoalDescription description;
    public ProjectedGoalInput(ProjectedModule module, ProjectedRegularGoalDescription description) {
      this.module = module;
      this.description = description;
    }
    @Override
    <R, P> R accept(GoalInputCases<R, P> cases, P p) {
      return cases.projected(this, p);
    }
  }

  static final Function<AbstractGoalInput, GoalContext> getContext =
      goalInputCases(
          projected -> projected.description.context,
          projected -> projected.description.context,
          projected -> projected.description.details.context);

  private DtoGeneratorInput() {
    throw new UnsupportedOperationException("no instances");
  }
}
