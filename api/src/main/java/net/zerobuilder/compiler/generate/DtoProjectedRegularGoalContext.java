package net.zerobuilder.compiler.generate;

import com.squareup.javapoet.CodeBlock;
import com.squareup.javapoet.TypeVariableName;
import net.zerobuilder.compiler.generate.DtoGoalDetails.AbstractRegularDetails;
import net.zerobuilder.compiler.generate.DtoGoalDetails.ConstructorGoalDetails;
import net.zerobuilder.compiler.generate.DtoGoalDetails.InstanceMethodGoalDetails;
import net.zerobuilder.compiler.generate.DtoGoalDetails.StaticMethodGoalDetails;
import net.zerobuilder.compiler.generate.DtoRegularGoalDescription.ProjectedRegularGoalDescription;

import java.util.List;
import java.util.function.Function;

import static java.util.Collections.emptyList;
import static net.zerobuilder.compiler.generate.DtoContext.ContextLifecycle.REUSE_INSTANCES;

public final class DtoProjectedRegularGoalContext {

  interface ProjectedRegularGoalContextCases<R> {
    R staticMethod(ProjectedMethodGoalContext staticMethod);
    R instanceMethod(ProjectedInstanceMethodGoalContext instanceMethod);
    R constructor(ProjectedConstructorGoalContext constructor);
  }

  public static abstract class ProjectedRegularGoalContext {

    private final ProjectedRegularGoalDescription description;

    public ProjectedRegularGoalDescription description() {
      return description;
    }

    public final Boolean mayReuse() {
      return mayReuse.apply(this);
    }

    ProjectedRegularGoalContext(ProjectedRegularGoalDescription description) {
      this.description = description;
    }

    public final DtoContext.GoalContext context() {
      return context.apply(this);
    }

    public final AbstractRegularDetails details() {
      return goalDetails.apply(this);
    }

    public final boolean isInstance() {
      return isInstance.apply(this);
    }

    public final List<TypeVariableName> instanceTypeParameters() {
      return instanceTypeParameters.apply(this);
    }

    abstract <R> R acceptRegularProjected(ProjectedRegularGoalContextCases<R> cases);

    public final CodeBlock invocationParameters() {
      return CodeBlock.of(String.join(", ", goalDetails.apply(this).parameterNames));
    }
  }

  private static final Function<ProjectedRegularGoalContext, DtoContext.GoalContext> context =
      projectedRegularGoalContextCases(
          staticMethod -> staticMethod.context,
          instanceMethod -> instanceMethod.context,
          constructor -> constructor.context);


  private static final Function<ProjectedRegularGoalContext, Boolean> mayReuse =
      projectedRegularGoalContextCases(
          staticMethod -> staticMethod.context.lifecycle == REUSE_INSTANCES
              && staticMethod.details.typeParameters.isEmpty(),
          instanceMethod -> false,
          constructor -> constructor.context.lifecycle == REUSE_INSTANCES
              && constructor.details.instanceTypeParameters.isEmpty());

  private static final Function<ProjectedRegularGoalContext, Boolean> isInstance =
      projectedRegularGoalContextCases(
          staticMethod -> false,
          instanceMethod -> true,
          constructor -> false);

  static <R> Function<ProjectedRegularGoalContext, R> asFunction(ProjectedRegularGoalContextCases<R> cases) {
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
    public final DtoContext.GoalContext context;
    public final StaticMethodGoalDetails details;

    ProjectedMethodGoalContext(
        DtoContext.GoalContext context,
        StaticMethodGoalDetails details,
        ProjectedRegularGoalDescription description) {
      super(description);
      this.context = context;
      this.details = details;
    }

    @Override
    public <R> R acceptRegularProjected(ProjectedRegularGoalContextCases<R> cases) {
      return cases.staticMethod(this);
    }
  }

  public static final class ProjectedInstanceMethodGoalContext extends ProjectedRegularGoalContext {
    public final DtoContext.GoalContext context;
    public final InstanceMethodGoalDetails details;

    ProjectedInstanceMethodGoalContext(
        DtoContext.GoalContext context,
        InstanceMethodGoalDetails details,
        ProjectedRegularGoalDescription description) {
      super(description);
      this.context = context;
      this.details = details;
    }

    @Override
    public <R> R acceptRegularProjected(ProjectedRegularGoalContextCases<R> cases) {
      return cases.instanceMethod(this);
    }
  }

  public static final class ProjectedConstructorGoalContext
      extends ProjectedRegularGoalContext {

    public final DtoContext.GoalContext context;
    public final ConstructorGoalDetails details;

    ProjectedConstructorGoalContext(DtoContext.GoalContext context,
                                    ConstructorGoalDetails details,
                                    ProjectedRegularGoalDescription description) {
      super(description);
      this.context = context;
      this.details = details;
    }

    @Override
    public <R> R acceptRegularProjected(ProjectedRegularGoalContextCases<R> cases) {
      return cases.constructor(this);
    }
  }

  private static final Function<ProjectedRegularGoalContext, AbstractRegularDetails> goalDetails =
      projectedRegularGoalContextCases(
          staticMethod -> staticMethod.details,
          instanceMethod -> instanceMethod.details,
          constructor -> constructor.details);

  private static final Function<ProjectedRegularGoalContext, List<TypeVariableName>> instanceTypeParameters =
      projectedRegularGoalContextCases(
          staticMethod -> emptyList(),
          instanceMethod -> instanceMethod.details.instanceTypeParameters,
          constructor -> constructor.details.instanceTypeParameters);

  private DtoProjectedRegularGoalContext() {
    throw new UnsupportedOperationException("no instances");
  }
}
