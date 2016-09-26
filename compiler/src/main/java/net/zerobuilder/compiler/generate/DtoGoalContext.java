package net.zerobuilder.compiler.generate;

import com.google.common.annotations.VisibleForTesting;
import com.google.common.base.Function;
import com.google.common.collect.ImmutableList;
import com.squareup.javapoet.ClassName;
import com.squareup.javapoet.TypeName;
import net.zerobuilder.compiler.generate.DtoBeanGoalContext.BeanGoalContext;
import net.zerobuilder.compiler.generate.DtoBuilders.BuildersContext;
import net.zerobuilder.compiler.generate.DtoRegularGoalContext.RegularGoalContext;
import net.zerobuilder.compiler.generate.DtoStep.AbstractStep;

import static net.zerobuilder.compiler.Utilities.upcase;
import static net.zerobuilder.compiler.generate.DtoRegularGoalContext.goalName;
import static net.zerobuilder.compiler.generate.DtoRegularGoalContext.goalType;

public final class DtoGoalContext {

  public static abstract class AbstractGoalContext {
    final BuildersContext builders;

    final boolean toBuilder;
    final boolean builder;

    final ClassName contractName;

    @VisibleForTesting
    AbstractGoalContext(BuildersContext builders,
                        boolean toBuilder,
                        boolean builder, ClassName contractName) {
      this.builders = builders;
      this.toBuilder = toBuilder;
      this.builder = builder;
      this.contractName = contractName;
    }

    abstract <R> R accept(GoalCases<R> cases);
  }

  interface GoalCases<R> {
    R regularGoal(RegularGoalContext goal);
    R beanGoal(BeanGoalContext goal);
  }

  static <R> GoalCases<R> goalCases(final Function<RegularGoalContext, R> regularFunction,
                                    final Function<BeanGoalContext, R> beanFunction) {
    return new GoalCases<R>() {
      @Override
      public R regularGoal(RegularGoalContext goal) {
        return regularFunction.apply(goal);
      }
      @Override
      public R beanGoal(BeanGoalContext goal) {
        return beanFunction.apply(goal);
      }
    };
  }

  static final class GoalContextCommon {
    final AbstractGoalContext goal;
    final TypeName goalType;
    final ImmutableList<? extends AbstractStep> parameters;
    final ImmutableList<TypeName> thrownTypes;
    private GoalContextCommon(AbstractGoalContext goal, TypeName goalType, ImmutableList<? extends AbstractStep> parameters,
                              ImmutableList<TypeName> thrownTypes) {
      this.goal = goal;
      this.goalType = goalType;
      this.parameters = parameters;
      this.thrownTypes = thrownTypes;
    }
  }

  static <R> GoalCases<R> always(final Function<GoalContextCommon, R> function) {
    return new GoalCases<R>() {
      @Override
      public R regularGoal(RegularGoalContext goal) {
        return function.apply(new GoalContextCommon(goal,
            goal.acceptRegular(goalType), goal.steps, goal.thrownTypes));
      }
      @Override
      public R beanGoal(BeanGoalContext goal) {
        ImmutableList<TypeName> thrownTypes = ImmutableList.of();
        return function.apply(new GoalContextCommon(goal, goal.goal.goalType, goal.steps, thrownTypes));
      }
    };
  }

  static <R> Function<AbstractGoalContext, R> goalCasesFunction(final GoalCases<R> cases) {
    return new Function<AbstractGoalContext, R>() {
      @Override
      public R apply(AbstractGoalContext goal) {
        return goal.accept(cases);
      }
    };
  }

  static final GoalCases<ImmutableList<ClassName>> stepInterfaceNames = always(new Function<GoalContextCommon, ImmutableList<ClassName>>() {
    @Override
    public ImmutableList<ClassName> apply(GoalContextCommon goal) {
      ImmutableList.Builder<ClassName> specs = ImmutableList.builder();
      for (AbstractStep abstractStep : goal.parameters) {
        specs.add(abstractStep.thisType);
      }
      return specs.build();
    }
  });

  static final GoalCases<ClassName> builderImplName = always(new Function<GoalContextCommon, ClassName>() {
    @Override
    public ClassName apply(GoalContextCommon goal) {
      return goal.goal.builders.generatedType.nestedClass(
          upcase(goal.goal.accept(getGoalName) + "BuilderImpl"));
    }
  });

  static final GoalCases<String> getGoalName = new GoalCases<String>() {
    @Override
    public String regularGoal(RegularGoalContext goal) {
      return goal.acceptRegular(goalName);
    }
    @Override
    public String beanGoal(BeanGoalContext goal) {
      return goal.goal.name;
    }
  };

  private DtoGoalContext() {
    throw new UnsupportedOperationException("no instances");
  }
}
