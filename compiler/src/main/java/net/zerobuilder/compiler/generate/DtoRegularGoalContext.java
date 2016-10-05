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

  static abstract class RegularGoalContext extends AbstractGoalContext {

    /**
     * original parameter order unless {@link net.zerobuilder.Step} was used
     */
    final ImmutableList<RegularStep> steps;
    final ImmutableList<TypeName> thrownTypes;

    RegularGoalContext(ImmutableList<RegularStep> steps,
                       ImmutableList<TypeName> thrownTypes) {
      this.thrownTypes = thrownTypes;
      this.steps = steps;
    }

    final <R> R accept(GoalCases<R> cases) {
      return cases.regularGoal(this);
    }
    abstract <R> R acceptRegular(RegularGoalContextCases<R> cases);
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

  public static abstract class ConstructorContext extends RegularGoalContext {
    final ConstructorGoal goal;

    public ConstructorContext(ConstructorGoal goal,
                              ImmutableList<RegularStep> steps,
                              ImmutableList<TypeName> thrownTypes) {
      super(steps, thrownTypes);
      this.goal = goal;
    }
  }

  public static final class ConstructorGoalContext extends ConstructorContext {
    final BuildersContext builders;

    public ConstructorGoalContext(ConstructorGoal goal,
                                  BuildersContext builders,
                                  ImmutableList<RegularStep> steps,
                                  ImmutableList<TypeName> thrownTypes) {
      super(goal, steps, thrownTypes);
      this.builders = builders;
    }

    @Override
    <R> R acceptRegular(RegularGoalContextCases<R> cases) {
      return cases.constructorGoal(this);
    }
  }

  public static abstract class MethodContext extends RegularGoalContext {
    final MethodGoal goal;

    public MethodContext(MethodGoal goal,
                         ImmutableList<RegularStep> steps,
                         ImmutableList<TypeName> thrownTypes) {
      super(steps, thrownTypes);
      this.goal = goal;
    }
  }

  public static final class MethodGoalContext extends MethodContext {
    final BuildersContext builders;

    public MethodGoalContext(MethodGoal goal,
                             BuildersContext builders,
                             ImmutableList<RegularStep> steps,
                             ImmutableList<TypeName> thrownTypes) {
      super(goal, steps, thrownTypes);
      this.builders = builders;
    }

    @Override
    <R> R acceptRegular(RegularGoalContextCases<R> cases) {
      return cases.methodGoal(this);
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

  private DtoRegularGoalContext() {
    throw new UnsupportedOperationException("no instances");
  }
}
