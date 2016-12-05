package net.zerobuilder.compiler.generate;

import net.zerobuilder.compiler.generate.DtoGoalDetails.ConstructorGoalDetails;
import net.zerobuilder.compiler.generate.DtoGoalDetails.InstanceMethodGoalDetails;
import net.zerobuilder.compiler.generate.DtoRegularGoalDescription.ProjectedRegularGoalDescription;

import java.util.function.Function;

public final class DtoProjectedRegularGoalContext {

  interface ProjectedRegularGoalContextCases<R> {
    R staticMethod(ProjectedMethodGoalContext staticMethod);
    R instanceMethod(ProjectedInstanceMethodGoalContext instanceMethod);
    R constructor(ProjectedConstructorGoalContext constructor);
  }

  public static abstract class ProjectedRegularGoalContext {

    public final ProjectedRegularGoalDescription description;

    ProjectedRegularGoalContext(ProjectedRegularGoalDescription description) {
      this.description = description;
    }

    abstract <R> R acceptRegularProjected(ProjectedRegularGoalContextCases<R> cases);
  }

  private static <R> Function<ProjectedRegularGoalContext, R> asFunction(ProjectedRegularGoalContextCases<R> cases) {
    return goal -> goal.acceptRegularProjected(cases);
  }

  public static <R> Function<ProjectedRegularGoalContext, R> projectedRegularGoalContextCases(
      Function<? super ProjectedMethodGoalContext, ? extends R> methodFunction,
      Function<? super ProjectedInstanceMethodGoalContext, ? extends R> instanceMethodFunction,
      Function<? super ProjectedConstructorGoalContext, ? extends R> constructorFunction) {
    return asFunction(new ProjectedRegularGoalContextCases<R>() {
      @Override
      public R staticMethod(ProjectedMethodGoalContext staticMethod) {
        return methodFunction.apply(staticMethod);
      }
      @Override
      public R instanceMethod(ProjectedInstanceMethodGoalContext instanceMethod) {
        return instanceMethodFunction.apply(instanceMethod);
      }
      @Override
      public R constructor(ProjectedConstructorGoalContext constructor) {
        return constructorFunction.apply(constructor);
      }
    });
  }

  public static final class ProjectedMethodGoalContext extends ProjectedRegularGoalContext {

    ProjectedMethodGoalContext(
        ProjectedRegularGoalDescription description) {
      super(description);
    }

    @Override
    public <R> R acceptRegularProjected(ProjectedRegularGoalContextCases<R> cases) {
      return cases.staticMethod(this);
    }
  }

  public static final class ProjectedInstanceMethodGoalContext extends ProjectedRegularGoalContext {
    public final InstanceMethodGoalDetails details;

    ProjectedInstanceMethodGoalContext(
        InstanceMethodGoalDetails details,
        ProjectedRegularGoalDescription description) {
      super(description);
      this.details = details;
    }

    @Override
    public <R> R acceptRegularProjected(ProjectedRegularGoalContextCases<R> cases) {
      return cases.instanceMethod(this);
    }
  }

  public static final class ProjectedConstructorGoalContext
      extends ProjectedRegularGoalContext {

    public final ConstructorGoalDetails details;

    ProjectedConstructorGoalContext(ConstructorGoalDetails details,
                                    ProjectedRegularGoalDescription description) {
      super(description);
      this.details = details;
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
