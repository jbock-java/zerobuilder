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

    RegularGoalContext(BuildersContext builders,
                       boolean toBuilder,
                       boolean builder,
                       ImmutableList<RegularStep> steps,
                       ImmutableList<TypeName> thrownTypes) {
      super(builders, toBuilder, builder);
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

  public final static class ConstructorGoalContext extends RegularGoalContext {
    final ConstructorGoal goal;

    public ConstructorGoalContext(ConstructorGoal goal,
                                  BuildersContext builders,
                                  boolean toBuilder,
                                  boolean builder,
                                  ImmutableList<RegularStep> steps,
                                  ImmutableList<TypeName> thrownTypes) {
      super(builders, toBuilder, builder, steps, thrownTypes);
      this.goal = goal;
    }

    @Override
    <R> R acceptRegular(RegularGoalContextCases<R> cases) {
      return cases.constructorGoal(this);
    }
  }

  public final static class MethodGoalContext extends RegularGoalContext {
    final MethodGoal goal;

    public MethodGoalContext(MethodGoal goal,
                             BuildersContext builders,
                             boolean toBuilder,
                             boolean builder,
                             ImmutableList<RegularStep> steps,
                             ImmutableList<TypeName> thrownTypes) {
      super(builders, toBuilder, builder, steps, thrownTypes);
      this.goal = goal;
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

  private DtoRegularGoalContext() {
    throw new UnsupportedOperationException("no instances");
  }
}
