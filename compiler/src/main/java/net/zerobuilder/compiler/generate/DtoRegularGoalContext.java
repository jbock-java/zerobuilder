package net.zerobuilder.compiler.generate;

import com.google.common.base.Function;
import com.google.common.collect.ImmutableList;
import com.squareup.javapoet.TypeName;
import net.zerobuilder.compiler.analyse.DtoGoal.ConstructorGoalDetails;
import net.zerobuilder.compiler.analyse.DtoGoal.MethodGoalDetails;
import net.zerobuilder.compiler.analyse.DtoGoal.RegularGoal;
import net.zerobuilder.compiler.generate.DtoBuilders.BuildersContext;
import net.zerobuilder.compiler.generate.DtoGoalContext.AbstractGoalContext;
import net.zerobuilder.compiler.generate.DtoGoalContext.GoalCases;
import net.zerobuilder.compiler.generate.DtoGoalContext.IGoal;
import net.zerobuilder.compiler.generate.DtoStep.RegularStep;

import java.util.List;

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

  public static final class ConstructorGoal implements IGoal {
    final ConstructorGoalDetails details;

    /**
     * original parameter order unless {@link net.zerobuilder.Step} was used
     */
    final ImmutableList<RegularStep> steps;
    final ImmutableList<TypeName> thrownTypes;

    private ConstructorGoal(ConstructorGoalDetails details,
                            ImmutableList<RegularStep> steps,
                            ImmutableList<TypeName> thrownTypes) {
      this.steps = steps;
      this.thrownTypes = thrownTypes;
      this.details = details;
    }

    public static ConstructorGoal create(ConstructorGoalDetails details,
                                         List<RegularStep> steps,
                                         List<TypeName> thrownTypes) {
      return new ConstructorGoal(details,
          ImmutableList.copyOf(steps),
          ImmutableList.copyOf(thrownTypes));
    }

    @Override
    public AbstractGoalContext withContext(BuildersContext context) {
      return new ConstructorGoalContext(this, context);
    }
  }

  static final class ConstructorGoalContext
      implements RegularGoalContext {

    final ConstructorGoal goal;
    final BuildersContext builders;

    ConstructorGoalContext(ConstructorGoal goal,
                           BuildersContext builders) {
      this.goal = goal;
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

  public static final class MethodGoal implements IGoal {
    final MethodGoalDetails details;

    /**
     * original parameter order unless {@link net.zerobuilder.Step} was used
     */
    final ImmutableList<RegularStep> steps;
    final ImmutableList<TypeName> thrownTypes;

    private MethodGoal(MethodGoalDetails details,
                       ImmutableList<RegularStep> steps,
                       ImmutableList<TypeName> thrownTypes) {
      this.steps = steps;
      this.thrownTypes = thrownTypes;
      this.details = details;
    }

    public static MethodGoal create(MethodGoalDetails details,
                                    List<RegularStep> steps,
                                    List<TypeName> thrownTypes) {
      return new MethodGoal(details,
          ImmutableList.copyOf(steps),
          ImmutableList.copyOf(thrownTypes));
    }

    @Override
    public AbstractGoalContext withContext(BuildersContext context) {
      return new MethodGoalContext(this, context);
    }
  }

  static final class MethodGoalContext
      implements RegularGoalContext {
    final BuildersContext builders;
    final MethodGoal goal;

    MethodGoalContext(MethodGoal goal,
                      BuildersContext builders) {
      this.goal = goal;
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
      return goal.goal.details;
    }
    @Override
    public RegularGoal methodGoal(MethodGoalContext goal) {
      return goal.goal.details;
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
      return goal.goal.details.instance;
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
      return goal.goal.thrownTypes;
    }
    @Override
    public ImmutableList<TypeName> methodGoal(MethodGoalContext goal) {
      return goal.goal.thrownTypes;
    }
  });

  static final Function<RegularGoalContext, ImmutableList<RegularStep>> regularSteps
      = asFunction(new RegularGoalContextCases<ImmutableList<RegularStep>>() {
    @Override
    public ImmutableList<RegularStep> constructorGoal(ConstructorGoalContext goal) {
      return goal.goal.steps;
    }
    @Override
    public ImmutableList<RegularStep> methodGoal(MethodGoalContext goal) {
      return goal.goal.steps;
    }
  });

  private DtoRegularGoalContext() {
    throw new UnsupportedOperationException("no instances");
  }
}
