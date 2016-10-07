package net.zerobuilder.compiler.generate;

import com.squareup.javapoet.ClassName;
import com.squareup.javapoet.TypeName;
import net.zerobuilder.compiler.generate.DtoBeanGoalContext.BeanGoalContext;
import net.zerobuilder.compiler.generate.DtoBuildersContext.BuildersContext;
import net.zerobuilder.compiler.generate.DtoGoal.AbstractGoalDetails;
import net.zerobuilder.compiler.generate.DtoGoal.RegularGoalDetails;
import net.zerobuilder.compiler.generate.DtoRegularGoalContext.RegularGoalContext;
import net.zerobuilder.compiler.generate.DtoStep.AbstractStep;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Function;

import static java.util.Collections.emptyList;
import static java.util.Collections.unmodifiableList;
import static net.zerobuilder.compiler.generate.DtoRegularGoalContext.regularSteps;
import static net.zerobuilder.compiler.generate.Utilities.upcase;

final class DtoGoalContext {

  interface IGoal {

    AbstractGoalContext withContext(BuildersContext context);
  }

  interface AbstractGoalContext {

    <R> R accept(GoalCases<R> cases);
  }

  interface GoalCases<R> {
    R regularGoal(RegularGoalContext goal);
    R beanGoal(BeanGoalContext goal);
  }

  static <R> Function<AbstractGoalContext, R> asFunction(final GoalCases<R> cases) {
    return goal -> goal.accept(cases);
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

  static List<ClassName> stepInterfaceTypes(AbstractGoalContext goal) {
    List<ClassName> specs = new ArrayList<>();
    for (AbstractStep abstractStep : abstractSteps.apply(goal)) {
      specs.add(abstractStep.thisType);
    }
    return specs;
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
          RegularGoalDetails regularGoalDetails = DtoRegularGoalContext.regularGoal.apply(goal);
          return regularGoalDetails.goalType;
        }
        @Override
        public TypeName beanGoal(BeanGoalContext goal) {
          return goal.goal.details.goalType;
        }
      });


  static final Function<AbstractGoalContext, String> goalName = asFunction(new GoalCases<String>() {
    @Override
    public String regularGoal(RegularGoalContext goal) {
      RegularGoalDetails regularGoalDetails = DtoRegularGoalContext.regularGoal.apply(goal);
      return regularGoalDetails.name;
    }
    @Override
    public String beanGoal(BeanGoalContext goal) {
      return goal.goal.details.name;
    }
  });


  static final Function<AbstractGoalContext, AbstractGoalDetails> abstractGoalDetails
      = asFunction(new GoalCases<AbstractGoalDetails>() {
    @Override
    public AbstractGoalDetails regularGoal(RegularGoalContext goal) {
      return DtoRegularGoalContext.regularGoal.apply(goal);
    }
    @Override
    public AbstractGoalDetails beanGoal(BeanGoalContext goal) {
      return goal.goal.details;
    }
  });

  static final Function<AbstractGoalContext, List<AbstractStep>> abstractSteps
      = asFunction(new GoalCases<List<AbstractStep>>() {
    @Override
    public List<AbstractStep> regularGoal(RegularGoalContext goal) {
      return unmodifiableList(regularSteps.apply(goal));
    }
    @Override
    public List<AbstractStep> beanGoal(BeanGoalContext goal) {
      return unmodifiableList(goal.goal.steps);
    }
  });

  static final Function<AbstractGoalContext, List<TypeName>> thrownTypes
      = asFunction(new GoalCases<List<TypeName>>() {
    @Override
    public List<TypeName> regularGoal(RegularGoalContext goal) {
      return DtoRegularGoalContext.thrownTypes.apply(goal);
    }
    @Override
    public List<TypeName> beanGoal(BeanGoalContext goal) {
      return emptyList();
    }
  });


  static ClassName contractName(String goalName, ClassName generatedType) {
    return generatedType.nestedClass(upcase(goalName + "Builder"));
  }

  private DtoGoalContext() {
    throw new UnsupportedOperationException("no instances");
  }
}
