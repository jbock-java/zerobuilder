package net.zerobuilder.compiler.generate;

import com.squareup.javapoet.TypeName;
import net.zerobuilder.compiler.generate.DtoBuildersContext.BuildersContext;
import net.zerobuilder.compiler.generate.DtoGoal.ConstructorGoalDetails;
import net.zerobuilder.compiler.generate.DtoGoal.MethodGoalDetails;
import net.zerobuilder.compiler.generate.DtoGoal.RegularGoalDetails;
import net.zerobuilder.compiler.generate.DtoGoalContext.AbstractGoalContext;
import net.zerobuilder.compiler.generate.DtoGoalContext.GoalCases;
import net.zerobuilder.compiler.generate.DtoGoalContext.IGoal;
import net.zerobuilder.compiler.generate.DtoStep.RegularStep;

import java.util.List;
import java.util.Set;
import java.util.function.Function;
import java.util.function.Predicate;

final class DtoRegularGoalContext {

  interface RegularGoalContext extends AbstractGoalContext {

    <R> R acceptRegular(RegularGoalContextCases<R> cases);
  }

  interface RegularGoalContextCases<R> {
    R constructorGoal(ConstructorGoalContext goal);
    R methodGoal(MethodGoalContext goal);
  }

  static <R> Function<RegularGoalContext, R> asFunction(final RegularGoalContextCases<R> cases) {
    return goal -> goal.acceptRegular(cases);
  }

  static Predicate<RegularGoalContext> asPredicate(final RegularGoalContextCases<Boolean> cases) {
    return goal -> goal.acceptRegular(cases);
  }

  static final class ConstructorGoal implements IGoal {
    final ConstructorGoalDetails details;

    final List<RegularStep> steps;
    final List<TypeName> thrownTypes;

    private ConstructorGoal(ConstructorGoalDetails details,
                            List<RegularStep> steps,
                            List<TypeName> thrownTypes) {
      this.steps = steps;
      this.thrownTypes = thrownTypes;
      this.details = details;
    }

    static ConstructorGoal create(ConstructorGoalDetails details,
                                  List<RegularStep> steps,
                                  List<TypeName> thrownTypes) {
      return new ConstructorGoal(details, steps, thrownTypes);
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

  static final class MethodGoal implements IGoal {
    final MethodGoalDetails details;

    final List<RegularStep> steps;
    final List<TypeName> thrownTypes;

    private MethodGoal(MethodGoalDetails details,
                       List<RegularStep> steps,
                       List<TypeName> thrownTypes) {
      this.steps = steps;
      this.thrownTypes = thrownTypes;
      this.details = details;
    }

    static MethodGoal create(MethodGoalDetails details,
                             List<RegularStep> steps,
                             List<TypeName> thrownTypes) {
      return new MethodGoal(details, steps, thrownTypes);
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

  static final Function<RegularGoalContext, RegularGoalDetails> regularGoal
      = asFunction(new RegularGoalContextCases<RegularGoalDetails>() {
    @Override
    public RegularGoalDetails constructorGoal(ConstructorGoalContext goal) {
      return goal.goal.details;
    }
    @Override
    public RegularGoalDetails methodGoal(MethodGoalContext goal) {
      return goal.goal.details;
    }
  });

  static final Predicate<RegularGoalContext> isInstance
      = asPredicate(new RegularGoalContextCases<Boolean>() {
    @Override
    public Boolean constructorGoal(ConstructorGoalContext goal) {
      return false;
    }
    @Override
    public Boolean methodGoal(MethodGoalContext goal) {
      return goal.goal.details.methodType.isInstance();
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

  static final Function<RegularGoalContext, List<TypeName>> thrownTypes
      = asFunction(new RegularGoalContextCases<List<TypeName>>() {
    @Override
    public List<TypeName> constructorGoal(ConstructorGoalContext goal) {
      return goal.goal.thrownTypes;
    }
    @Override
    public List<TypeName> methodGoal(MethodGoalContext goal) {
      return goal.goal.thrownTypes;
    }
  });

  static final Function<RegularGoalContext, List<RegularStep>> regularSteps
      = asFunction(new RegularGoalContextCases<List<RegularStep>>() {
    @Override
    public List<RegularStep> constructorGoal(ConstructorGoalContext goal) {
      return goal.goal.steps;
    }
    @Override
    public List<RegularStep> methodGoal(MethodGoalContext goal) {
      return goal.goal.steps;
    }
  });

  private DtoRegularGoalContext() {
    throw new UnsupportedOperationException("no instances");
  }
}
