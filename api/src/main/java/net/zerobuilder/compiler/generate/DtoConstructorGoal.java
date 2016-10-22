package net.zerobuilder.compiler.generate;

import com.squareup.javapoet.TypeName;
import net.zerobuilder.compiler.generate.DtoContext.BuildersContext;
import net.zerobuilder.compiler.generate.DtoGoal.ConstructorGoalDetails;
import net.zerobuilder.compiler.generate.DtoRegularGoal.RegularGoalContextCases;
import net.zerobuilder.compiler.generate.DtoRegularStep.AbstractRegularStep;
import net.zerobuilder.compiler.generate.DtoRegularStep.ProjectedRegularStep;
import net.zerobuilder.compiler.generate.DtoRegularStep.SimpleRegularStep;

import java.util.List;
import java.util.function.Function;

import static java.util.Collections.unmodifiableList;

public class DtoConstructorGoal {

  interface ConstructorGoalCases<R> {
    R simple(SimpleConstructorGoalContext simple);
    R projected(ProjectedConstructorGoalContext projected);
  }

  static <R> Function<AbstractConstructorGoalContext, R> asFunction(ConstructorGoalCases<R> cases) {
    return goal -> goal.acceptConstructor(cases);
  }

  static <R> Function<AbstractConstructorGoalContext, R> constructorGoalCases(
      Function<SimpleConstructorGoalContext, R> simpleFunction,
      Function<ProjectedConstructorGoalContext, R> projectedFunction) {
    return asFunction(new ConstructorGoalCases<R>() {
      @Override
      public R simple(SimpleConstructorGoalContext simple) {
        return simpleFunction.apply(simple);
      }
      @Override
      public R projected(ProjectedConstructorGoalContext projected) {
        return projectedFunction.apply(projected);
      }
    });
  }

  static abstract class AbstractConstructorGoalContext
      extends DtoRegularGoal.AbstractRegularGoalContext {

    final BuildersContext context;
    final ConstructorGoalDetails details;

    final List<TypeName> thrownTypes;

    AbstractConstructorGoalContext(BuildersContext context,
                                   ConstructorGoalDetails details,
                                   List<TypeName> thrownTypes) {
      this.context = context;
      this.details = details;
      this.thrownTypes = thrownTypes;
    }

    final List<AbstractRegularStep> constructorSteps() {
      return steps.apply(this);
    }

    @Override
    public final <R> R acceptRegular(RegularGoalContextCases<R> cases) {
      return cases.constructorGoal(this);
    }

    @Override
    public final List<String> parameterNames() {
      return details.parameterNames;
    }

    @Override
    public final TypeName type() {
      return details.goalType;
    }

    @Override
    public final <R> R accept(DtoGoalContext.GoalCases<R> cases) {
      return cases.regularGoal(this);
    }

    public abstract <R> R acceptConstructor(ConstructorGoalCases<R> cases);
  }

  private static final Function<AbstractConstructorGoalContext, List<AbstractRegularStep>> steps =
      constructorGoalCases(
          simple -> unmodifiableList(simple.steps),
          projected -> unmodifiableList(projected.steps));

  static final class SimpleConstructorGoalContext
      extends AbstractConstructorGoalContext {

    final List<SimpleRegularStep> steps;

    SimpleConstructorGoalContext(BuildersContext context,
                                 ConstructorGoalDetails details,
                                 List<SimpleRegularStep> steps,
                                 List<TypeName> thrownTypes) {
      super(context, details, thrownTypes);
      this.steps = steps;
    }

    @Override
    public <R> R acceptConstructor(ConstructorGoalCases<R> cases) {
      return cases.simple(this);
    }
  }

  static final class ProjectedConstructorGoalContext
      extends AbstractConstructorGoalContext {

    final List<ProjectedRegularStep> steps;

    ProjectedConstructorGoalContext(BuildersContext context,
                                    ConstructorGoalDetails details,
                                    List<ProjectedRegularStep> steps,
                                    List<TypeName> thrownTypes) {
      super(context, details, thrownTypes);
      this.steps = steps;
    }

    @Override
    public <R> R acceptConstructor(ConstructorGoalCases<R> cases) {
      return cases.projected(this);
    }
  }

  private DtoConstructorGoal() {
    throw new UnsupportedOperationException("no instances");
  }
}
