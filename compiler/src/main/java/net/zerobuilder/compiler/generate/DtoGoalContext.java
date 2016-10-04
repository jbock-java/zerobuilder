package net.zerobuilder.compiler.generate;

import com.google.common.annotations.VisibleForTesting;
import com.google.common.base.Function;
import com.google.common.collect.ImmutableList;
import com.squareup.javapoet.ClassName;
import com.squareup.javapoet.TypeName;
import net.zerobuilder.compiler.analyse.DtoGoal.RegularGoal;
import net.zerobuilder.compiler.generate.DtoBeanGoalContext.BeanGoalContext;
import net.zerobuilder.compiler.generate.DtoBuilders.BuildersContext;
import net.zerobuilder.compiler.generate.DtoRegularGoalContext.RegularGoalContext;
import net.zerobuilder.compiler.generate.DtoStep.AbstractStep;

import static net.zerobuilder.compiler.Utilities.upcase;

public final class DtoGoalContext {

  public static abstract class AbstractGoalContext {
    final BuildersContext builders;

    final boolean toBuilder;
    final boolean builder;

    final ClassName builderContractType;

    @VisibleForTesting
    AbstractGoalContext(BuildersContext builders, boolean toBuilder,
                        boolean builder, ClassName builderContractType) {
      this.builders = builders;
      this.toBuilder = toBuilder;
      this.builder = builder;
      this.builderContractType = builderContractType;
    }

    abstract <R> R accept(GoalCases<R> cases);
  }

  interface GoalCases<R> {
    R regularGoal(RegularGoalContext goal);
    R beanGoal(BeanGoalContext goal);
  }

  static <R> Function<AbstractGoalContext, R> asFunction(final GoalCases<R> cases) {
    return new Function<AbstractGoalContext, R>() {
      @Override
      public R apply(AbstractGoalContext goal) {
        return goal.accept(cases);
      }
    };
  }

  static <R> Function<AbstractGoalContext, R>
  goalCases(final Function<RegularGoalContext, R> regularFunction,
            final Function<BeanGoalContext, R> beanFunction) {
    return asFunction(new GoalCases<R>() {
      @Override
      public R regularGoal(RegularGoalContext goal) {
        return regularFunction.apply(goal);
      }
      @Override
      public R beanGoal(BeanGoalContext goal) {
        return beanFunction.apply(goal);
      }
    });
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

  static <R> Function<AbstractGoalContext, R> always(final Function<GoalContextCommon, R> function) {
    return asFunction(new GoalCases<R>() {
      @Override
      public R regularGoal(RegularGoalContext goal) {
        RegularGoal regularGoal = DtoRegularGoalContext.regularGoal.apply(goal);
        return function.apply(new GoalContextCommon(goal,
            regularGoal.goalType, goal.steps, goal.thrownTypes));
      }
      @Override
      public R beanGoal(BeanGoalContext goal) {
        ImmutableList<TypeName> thrownTypes = ImmutableList.of();
        return function.apply(new GoalContextCommon(goal, goal.goal.goalType, goal.steps, thrownTypes));
      }
    });
  }

  static final Function<AbstractGoalContext, ImmutableList<ClassName>> stepInterfaceTypes
      = always(new Function<GoalContextCommon, ImmutableList<ClassName>>() {
    @Override
    public ImmutableList<ClassName> apply(GoalContextCommon goal) {
      ImmutableList.Builder<ClassName> specs = ImmutableList.builder();
      for (AbstractStep abstractStep : goal.parameters) {
        specs.add(abstractStep.thisType);
      }
      return specs.build();
    }
  });

  static final Function<AbstractGoalContext, ClassName> builderImplType
      = always(new Function<GoalContextCommon, ClassName>() {
    @Override
    public ClassName apply(GoalContextCommon goal) {
      return goal.goal.builders.generatedType.nestedClass(
          upcase(goalName.apply(goal.goal) + "BuilderImpl"));
    }
  });

  static final Function<AbstractGoalContext, String> goalName = asFunction(new GoalCases<String>() {
    @Override
    public String regularGoal(RegularGoalContext goal) {
      RegularGoal regularGoal = DtoRegularGoalContext.regularGoal.apply(goal);
      return regularGoal.name;
    }
    @Override
    public String beanGoal(BeanGoalContext goal) {
      return goal.goal.name;
    }
  });

  private DtoGoalContext() {
    throw new UnsupportedOperationException("no instances");
  }
}
