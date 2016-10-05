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
    final boolean toBuilder;
    final boolean builder;

    @VisibleForTesting
    AbstractGoalContext(boolean toBuilder,
                        boolean builder) {
      this.toBuilder = toBuilder;
      this.builder = builder;
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

  static ImmutableList<ClassName> stepInterfaceTypes(AbstractGoalContext goal) {
    ImmutableList.Builder<ClassName> specs = ImmutableList.builder();
    for (AbstractStep abstractStep : abstractSteps.apply(goal)) {
      specs.add(abstractStep.thisType);
    }
    return specs.build();
  }

  static final Function<AbstractGoalContext, BuildersContext> buildersContext
      = asFunction(new GoalCases<BuildersContext>() {
    @Override
    public BuildersContext regularGoal(RegularGoalContext goal) {
      return DtoRegularGoalContext.buildersContext.apply(goal);
    }
    @Override
    public BuildersContext beanGoal(BeanGoalContext goal) {
      return goal.builders;
    }
  });

  static ClassName builderImplType(AbstractGoalContext goal) {
    return buildersContext.apply(goal).generatedType.nestedClass(
        upcase(goalName.apply(goal) + "BuilderImpl"));
  }

  static final Function<AbstractGoalContext, TypeName> goalType =
      asFunction(new GoalCases<TypeName>() {
        @Override
        public TypeName regularGoal(RegularGoalContext goal) {
          RegularGoal regularGoal = DtoRegularGoalContext.regularGoal.apply(goal);
          return regularGoal.goalType;
        }
        @Override
        public TypeName beanGoal(BeanGoalContext goal) {
          return goal.goal.goalType;
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


  static final Function<AbstractGoalContext, ImmutableList<? extends AbstractStep>> abstractSteps
      = asFunction(new GoalCases<ImmutableList<? extends AbstractStep>>() {
    @Override
    public ImmutableList<? extends AbstractStep> regularGoal(RegularGoalContext goal) {
      return goal.steps;
    }
    @Override
    public ImmutableList<? extends AbstractStep> beanGoal(BeanGoalContext goal) {
      return goal.steps;
    }
  });

  static final Function<AbstractGoalContext, ImmutableList<TypeName>> thrownTypes
      = asFunction(new GoalCases<ImmutableList<TypeName>>() {
    @Override
    public ImmutableList<TypeName> regularGoal(RegularGoalContext goal) {
      return goal.thrownTypes;
    }
    @Override
    public ImmutableList<TypeName> beanGoal(BeanGoalContext goal) {
      return ImmutableList.of();
    }
  });


  public static ClassName contractName(String goalName, BuildersContext buildersContext) {
    return buildersContext.generatedType.nestedClass(upcase(goalName + "Builder"));
  }

  private DtoGoalContext() {
    throw new UnsupportedOperationException("no instances");
  }
}
