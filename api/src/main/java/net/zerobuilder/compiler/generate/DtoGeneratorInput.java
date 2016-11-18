package net.zerobuilder.compiler.generate;

import net.zerobuilder.compiler.generate.DtoContext.GoalContext;
import net.zerobuilder.compiler.generate.DtoDescriptionInput.DescriptionInput;
import net.zerobuilder.compiler.generate.DtoMethodGoal.SimpleStaticMethodGoalContext;
import net.zerobuilder.compiler.generate.DtoModule.Module;
import net.zerobuilder.compiler.generate.DtoModule.ProjectedModule;
import net.zerobuilder.compiler.generate.DtoModule.RegularContractModule;
import net.zerobuilder.compiler.generate.DtoModule.RegularSimpleModule;
import net.zerobuilder.compiler.generate.DtoProjectedGoal.ProjectedGoal;
import net.zerobuilder.compiler.generate.DtoRegularGoal.SimpleRegularGoalContext;
import net.zerobuilder.compiler.generate.DtoSimpleGoal.SimpleGoal;

import java.util.List;
import java.util.function.Function;

public final class DtoGeneratorInput {

  interface GoalInputCases<R> {
    R simple(GoalInput simple);
    R projected(ProjectedGoalInput projected);
    R simpleRegular(SimpleRegularGoalInput simpleRegular);
    R regularSimple(RegularSimpleGoalInput regularSimple);
  }

  static abstract class AbstractGoalInput {
    abstract <R> R accept(GoalInputCases<R> cases);
  }

  static <R> Function<AbstractGoalInput, R> asFunction(GoalInputCases<R> cases) {
    return input -> input.accept(cases);
  }

  static <R> Function<AbstractGoalInput, R> goalInputCases(
      Function<GoalInput, R> simpleFunction,
      Function<ProjectedGoalInput, R> projectedFunction,
      Function<SimpleRegularGoalInput, R> simpleRegularFunction,
      Function<RegularSimpleGoalInput, R> regularSimpleFunction) {
    return asFunction(new GoalInputCases<R>() {
      @Override
      public R simple(GoalInput simple) {
        return simpleFunction.apply(simple);
      }
      @Override
      public R projected(ProjectedGoalInput projected) {
        return projectedFunction.apply(projected);
      }
      @Override
      public R simpleRegular(SimpleRegularGoalInput simpleRegular) {
        return simpleRegularFunction.apply(simpleRegular);
      }
      @Override
      public R regularSimple(RegularSimpleGoalInput regularSimple) {
        return regularSimpleFunction.apply(regularSimple);
      }
    });
  }

  static final class GoalInput extends AbstractGoalInput {
    final Module module;
    final SimpleGoal goal;
    GoalInput(Module module, SimpleGoal goal) {
      this.module = module;
      this.goal = goal;
    }
    @Override
    <R> R accept(GoalInputCases<R> cases) {
      return cases.simple(this);
    }
  }

  /**
   * @deprecated use {@link RegularSimpleGoalInput}
   */
  @Deprecated
  static final class SimpleRegularGoalInput extends AbstractGoalInput {
    final RegularContractModule module;
    final SimpleStaticMethodGoalContext goal;
    SimpleRegularGoalInput(RegularContractModule module, SimpleStaticMethodGoalContext goal) {
      this.module = module;
      this.goal = goal;
    }
    @Override
    <R> R accept(GoalInputCases<R> cases) {
      return cases.simpleRegular(this);
    }
  }

  static final class RegularSimpleGoalInput extends AbstractGoalInput {
    final RegularSimpleModule module;
    final SimpleRegularGoalContext goal;
    RegularSimpleGoalInput(RegularSimpleModule module, SimpleRegularGoalContext goal) {
      this.module = module;
      this.goal = goal;
    }
    @Override
    <R> R accept(GoalInputCases<R> cases) {
      return cases.regularSimple(this);
    }
  }

  static final class ProjectedGoalInput extends AbstractGoalInput {
    final ProjectedModule module;
    final ProjectedGoal goal;
    ProjectedGoalInput(ProjectedModule module, ProjectedGoal goal) {
      this.module = module;
      this.goal = goal;
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
