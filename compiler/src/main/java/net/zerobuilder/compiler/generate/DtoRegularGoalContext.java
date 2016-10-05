package net.zerobuilder.compiler.generate;

import com.google.common.base.Function;
import com.google.common.collect.ImmutableList;
import com.squareup.javapoet.TypeName;
import net.zerobuilder.compiler.analyse.DtoGoal.ConstructorGoal;
import net.zerobuilder.compiler.analyse.DtoGoal.MethodGoal;
import net.zerobuilder.compiler.analyse.DtoGoal.RegularGoal;
import net.zerobuilder.compiler.generate.DtoBuilders.BuildersContext;
import net.zerobuilder.compiler.generate.DtoGoalContext.AbstractGoalContext;
import net.zerobuilder.compiler.generate.DtoGoalContext.GoalCases;
import net.zerobuilder.compiler.generate.DtoStep.RegularStep;

public final class DtoRegularGoalContext {

  interface RegularGoalContext extends AbstractGoalContext {

    <R> R acceptRegular(RegularGoalContextCases<R> cases);
  }

  interface RegularGoalContextCases<R> {
    R constructorGoal(ConstructorGoalContext goal);
    R methodGoal(MethodGoalContext goal);
  }

  static <R> Function<RegularGoalContext, R> asFunction(final RegularGoalContextCases<R> cases) {
    return new Function<RegularGoalContext, R>() {
      @Override
      public R apply(RegularGoalContext goal) {
        return goal.acceptRegular(cases);
      }
    };
  }

  public static abstract class ConstructorContext {
    final ConstructorGoal goal;

    /**
     * original parameter order unless {@link net.zerobuilder.Step} was used
     */
    final ImmutableList<RegularStep> steps;
    final ImmutableList<TypeName> thrownTypes;

    public ConstructorContext(ConstructorGoal goal,
                              ImmutableList<RegularStep> steps,
                              ImmutableList<TypeName> thrownTypes) {
      this.steps = steps;
      this.thrownTypes = thrownTypes;
      this.goal = goal;
    }
  }

  public static final class ConstructorGoalContext extends ConstructorContext
      implements RegularGoalContext {
    final BuildersContext builders;

    public ConstructorGoalContext(ConstructorGoal goal,
                                  BuildersContext builders,
                                  ImmutableList<RegularStep> steps,
                                  ImmutableList<TypeName> thrownTypes) {
      super(goal, steps, thrownTypes);
      this.builders = builders;
    }

    @Override
    public <R> R acceptRegular(RegularGoalContextCases<R> cases) {
      return cases.constructorGoal(this);
    }

    @Override
    public <R> R accept(GoalCases<R> cases) {
      return cases.regularGoal(this);
    }
  }

  public static abstract class MethodContext {
    final MethodGoal goal;

    /**
     * original parameter order unless {@link net.zerobuilder.Step} was used
     */
    final ImmutableList<RegularStep> steps;
    final ImmutableList<TypeName> thrownTypes;

    public MethodContext(MethodGoal goal,
                         ImmutableList<RegularStep> steps,
                         ImmutableList<TypeName> thrownTypes) {
      this.steps = steps;
      this.thrownTypes = thrownTypes;
      this.goal = goal;
    }
  }

  public static final class MethodGoalContext extends MethodContext
      implements RegularGoalContext {
    final BuildersContext builders;

    public MethodGoalContext(MethodGoal goal,
                             BuildersContext builders,
                             ImmutableList<RegularStep> steps,
                             ImmutableList<TypeName> thrownTypes) {
      super(goal, steps, thrownTypes);
      this.builders = builders;
    }

    @Override
    public <R> R acceptRegular(RegularGoalContextCases<R> cases) {
      return cases.methodGoal(this);
    }

    @Override
    public <R> R accept(GoalCases<R> cases) {
      return cases.regularGoal(this);
    }
  }

  static final Function<RegularGoalContext, RegularGoal> regularGoal
      = asFunction(new RegularGoalContextCases<RegularGoal>() {
    @Override
    public RegularGoal constructorGoal(ConstructorGoalContext goal) {
      return goal.goal;
    }
    @Override
    public RegularGoal methodGoal(MethodGoalContext goal) {
      return goal.goal;
    }
  });

  static final Function<RegularGoalContext, Boolean> isInstance
      = asFunction(new RegularGoalContextCases<Boolean>() {
    @Override
    public Boolean constructorGoal(ConstructorGoalContext goal) {
      return false;
    }
    @Override
    public Boolean methodGoal(MethodGoalContext goal) {
      return goal.goal.instance;
    }
  });

  static final Function<RegularGoalContext, BuildersContext> buildersContext
      = asFunction(new RegularGoalContextCases<BuildersContext>() {
    @Override
    public BuildersContext constructorGoal(ConstructorGoalContext goal) {
      return goal.builders;
    }
    @Override
    public BuildersContext methodGoal(MethodGoalContext goal) {
      return goal.builders;
    }
  });

  static final Function<RegularGoalContext, ImmutableList<TypeName>> thrownTypes
      = asFunction(new RegularGoalContextCases<ImmutableList<TypeName>>() {
    @Override
    public ImmutableList<TypeName> constructorGoal(ConstructorGoalContext goal) {
      return goal.thrownTypes;
    }
    @Override
    public ImmutableList<TypeName> methodGoal(MethodGoalContext goal) {
      return goal.thrownTypes;
    }
  });

  static final Function<RegularGoalContext, ImmutableList<RegularStep>> regularSteps
      = asFunction(new RegularGoalContextCases<ImmutableList<RegularStep>>() {
    @Override
    public ImmutableList<RegularStep> constructorGoal(ConstructorGoalContext goal) {
      return goal.steps;
    }
    @Override
    public ImmutableList<RegularStep> methodGoal(MethodGoalContext goal) {
      return goal.steps;
    }
  });

  private DtoRegularGoalContext() {
    throw new UnsupportedOperationException("no instances");
  }
}
