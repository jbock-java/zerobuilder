package net.zerobuilder.compiler.generate;

import com.squareup.javapoet.TypeName;
import net.zerobuilder.compiler.generate.DtoContext.BuildersContext;
import net.zerobuilder.compiler.generate.DtoGoal.MethodGoalDetails;
import net.zerobuilder.compiler.generate.DtoMethodGoal.AbstractMethodGoalContext;
import net.zerobuilder.compiler.generate.DtoProjectedGoal.ProjectedGoal;
import net.zerobuilder.compiler.generate.DtoProjectedGoal.ProjectedGoalCases;

import java.util.List;
import java.util.function.Function;

import static net.zerobuilder.compiler.generate.DtoRegularStep.ProjectedRegularStep;

public final class DtoProjectedRegularGoalContext {

  interface ProjectedRegularGoalContextCases<R> {
    R method(ProjectedMethodGoalContext method);
    R constructor(ProjectedConstructorGoalContext constructor);
  }

  interface ProjectedRegularGoalContext extends ProjectedGoal {
    <R> R acceptRegularProjected(ProjectedRegularGoalContextCases<R> cases);
  }

  static <R> Function<ProjectedRegularGoalContext, R> asFunction(ProjectedRegularGoalContextCases<R> cases) {
    return goal -> goal.acceptRegularProjected(cases);
  }

  static <R> Function<ProjectedRegularGoalContext, R> projectedRegularGoalContextCases(
      Function<ProjectedMethodGoalContext, R> methodFunction,
      Function<ProjectedConstructorGoalContext, R> constructorFunction) {
    return asFunction(new ProjectedRegularGoalContextCases<R>() {
      @Override
      public R method(ProjectedMethodGoalContext method) {
        return methodFunction.apply(method);
      }
      @Override
      public R constructor(ProjectedConstructorGoalContext constructor) {
        return constructorFunction.apply(constructor);
      }
    });
  }

  static final class ProjectedMethodGoalContext extends AbstractMethodGoalContext
      implements ProjectedRegularGoalContext {
    final List<ProjectedRegularStep> steps;

    ProjectedMethodGoalContext(
        BuildersContext context,
        MethodGoalDetails details,
        List<ProjectedRegularStep> steps,
        List<TypeName> thrownTypes) {
      super(context, details, thrownTypes);
      this.steps = steps;
    }

    @Override
    public <R> R acceptMethod(DtoMethodGoal.MethodGoalCases<R> cases) {
      return cases.projected(this);
    }

    @Override
    public <R> R acceptProjected(ProjectedGoalCases<R> cases) {
      return cases.method(this);
    }

    @Override
    public <R> R acceptRegularProjected(ProjectedRegularGoalContextCases<R> cases) {
      return cases.method(this);
    }
  }

  static final class ProjectedConstructorGoalContext
      extends DtoConstructorGoal.AbstractConstructorGoalContext
      implements ProjectedRegularGoalContext {

    final List<ProjectedRegularStep> steps;

    ProjectedConstructorGoalContext(BuildersContext context,
                                    DtoGoal.ConstructorGoalDetails details,
                                    List<ProjectedRegularStep> steps,
                                    List<TypeName> thrownTypes) {
      super(context, details, thrownTypes);
      this.steps = steps;
    }

    @Override
    public <R> R acceptConstructor(DtoConstructorGoal.ConstructorGoalCases<R> cases) {
      return cases.projected(this);
    }

    @Override
    public <R> R acceptProjected(ProjectedGoalCases<R> cases) {
      return cases.constructor(this);
    }

    @Override
    public <R> R acceptRegularProjected(ProjectedRegularGoalContextCases<R> cases) {
      return cases.constructor(this);
    }
  }


  private DtoProjectedRegularGoalContext() {
    throw new UnsupportedOperationException("no instances");
  }
}
