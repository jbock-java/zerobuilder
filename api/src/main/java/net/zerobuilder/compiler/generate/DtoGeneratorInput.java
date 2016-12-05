package net.zerobuilder.compiler.generate;

import net.zerobuilder.compiler.generate.DtoBeanGoal.BeanGoalContext;
import net.zerobuilder.compiler.generate.DtoContext.GoalContext;
import net.zerobuilder.compiler.generate.DtoDescriptionInput.DescriptionInput;
import net.zerobuilder.compiler.generate.DtoModule.BeanModule;
import net.zerobuilder.compiler.generate.DtoModule.ProjectedModule;
import net.zerobuilder.compiler.generate.DtoModule.RegularSimpleModule;
import net.zerobuilder.compiler.generate.DtoRegularGoalDescription.ProjectedRegularGoalDescription;
import net.zerobuilder.compiler.generate.DtoRegularGoalDescription.SimpleRegularGoalDescription;

import java.util.List;
import java.util.function.Function;

public final class DtoGeneratorInput {

  interface GoalInputCases<R> {
    R projected(ProjectedGoalInput projected);
    R regularSimple(RegularSimpleGoalInput regularSimple);
    R bean(BeanGoalInput bean);
  }

  static abstract class AbstractGoalInput {
    abstract <R> R accept(GoalInputCases<R> cases);
  }

  private static <R> Function<AbstractGoalInput, R> asFunction(GoalInputCases<R> cases) {
    return input -> input.accept(cases);
  }

  static <R> Function<AbstractGoalInput, R> goalInputCases(
      Function<ProjectedGoalInput, R> projectedFunction,
      Function<RegularSimpleGoalInput, R> regularSimpleFunction,
      Function<BeanGoalInput, R> beanFunction) {
    return asFunction(new GoalInputCases<R>() {
      @Override
      public R projected(ProjectedGoalInput projected) {
        return projectedFunction.apply(projected);
      }
      @Override
      public R regularSimple(RegularSimpleGoalInput regularSimple) {
        return regularSimpleFunction.apply(regularSimple);
      }
      @Override
      public R bean(BeanGoalInput bean) {
        return beanFunction.apply(bean);
      }
    });
  }

  static final class RegularSimpleGoalInput extends AbstractGoalInput {
    final RegularSimpleModule module;
    final SimpleRegularGoalDescription description;
    RegularSimpleGoalInput(RegularSimpleModule module, SimpleRegularGoalDescription description) {
      this.module = module;
      this.description = description;
    }
    @Override
    <R> R accept(GoalInputCases<R> cases) {
      return cases.regularSimple(this);
    }
  }

  static final class BeanGoalInput extends AbstractGoalInput {
    final BeanModule module;
    final BeanGoalContext goal;
    BeanGoalInput(BeanModule module, BeanGoalContext goal) {
      this.module = module;
      this.goal = goal;
    }
    @Override
    <R> R accept(GoalInputCases<R> cases) {
      return cases.bean(this);
    }
  }

  static final class ProjectedGoalInput extends AbstractGoalInput {
    final ProjectedModule module;
    final ProjectedRegularGoalDescription description;
    ProjectedGoalInput(ProjectedModule module, ProjectedRegularGoalDescription description) {
      this.module = module;
      this.description = description;
    }
    @Override
    <R> R accept(GoalInputCases<R> cases) {
      return cases.projected(this);
    }
  }

  public static final class GeneratorInput {
    public final List<DescriptionInput> goals;
    public final GoalContext context;

    private GeneratorInput(DtoContext.GoalContext context, List<DescriptionInput> goals) {
      this.goals = goals;
      this.context = context;
    }

    public static GeneratorInput create(GoalContext context, List<DescriptionInput> goals) {
      return new GeneratorInput(context, goals);
    }
  }

  private DtoGeneratorInput() {
    throw new UnsupportedOperationException("no instances");
  }
}
