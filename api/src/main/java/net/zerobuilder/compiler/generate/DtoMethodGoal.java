package net.zerobuilder.compiler.generate;

import com.squareup.javapoet.ClassName;
import com.squareup.javapoet.FieldSpec;
import com.squareup.javapoet.TypeName;
import net.zerobuilder.compiler.generate.DtoContext.BuildersContext;
import net.zerobuilder.compiler.generate.DtoGoal.GoalMethodType;
import net.zerobuilder.compiler.generate.DtoGoal.MethodGoalDetails;
import net.zerobuilder.compiler.generate.DtoRegularGoal.AbstractRegularGoalContext;
import net.zerobuilder.compiler.generate.DtoRegularStep.AbstractRegularStep;

import java.util.List;
import java.util.function.Function;
import java.util.function.Supplier;

import static java.util.Collections.unmodifiableList;
import static javax.lang.model.element.Modifier.FINAL;
import static javax.lang.model.element.Modifier.PRIVATE;
import static net.zerobuilder.compiler.generate.DtoContext.BuilderLifecycle.REUSE_INSTANCES;
import static net.zerobuilder.compiler.generate.DtoRegularStep.ProjectedRegularStep;
import static net.zerobuilder.compiler.generate.DtoRegularStep.SimpleRegularStep;
import static net.zerobuilder.compiler.generate.Utilities.downcase;
import static net.zerobuilder.compiler.generate.Utilities.fieldSpec;
import static net.zerobuilder.compiler.generate.Utilities.memoize;

public final class DtoMethodGoal {

  interface MethodGoalCases<R> {
    R simple(SimpleMethodGoalContext simple);
    R projected(ProjectedMethodGoalContext projected);
  }

  static <R> Function<AbstractMethodGoalContext, R> asFunction(MethodGoalCases<R> cases) {
    return goal -> goal.acceptMethod(cases);
  }

  static <R> Function<AbstractMethodGoalContext, R> methodGoalCases(
      Function<SimpleMethodGoalContext, R> simpleFunction,
      Function<ProjectedMethodGoalContext, R> projectedFunction) {
    return asFunction(new MethodGoalCases<R>() {
      @Override
      public R simple(SimpleMethodGoalContext simple) {
        return simpleFunction.apply(simple);
      }
      @Override
      public R projected(ProjectedMethodGoalContext projected) {
        return projectedFunction.apply(projected);
      }
    });
  }

  static abstract class AbstractMethodGoalContext extends AbstractRegularGoalContext {

    final BuildersContext context;
    final MethodGoalDetails details;
    final List<TypeName> thrownTypes;

    GoalMethodType methodType() {
      return details.methodType;
    }

    private final Supplier<FieldSpec> field;

    /**
     * @return An instance of type {@link BuildersContext#type}.
     */
    FieldSpec field() {
      return field.get();
    }

    AbstractMethodGoalContext(BuildersContext context,
                              MethodGoalDetails details,
                              List<TypeName> thrownTypes) {
      this.details = details;
      this.thrownTypes = thrownTypes;
      this.context = context;
      this.field = memoizeField(context);
    }

    final List<AbstractRegularStep> methodSteps() {
      return steps.apply(this);
    }

    private static Supplier<FieldSpec> memoizeField(BuildersContext context) {
      return memoize(() -> {
        ClassName type = context.type;
        String name = '_' + downcase(type.simpleName());
        return context.lifecycle == REUSE_INSTANCES
            ? fieldSpec(type, name, PRIVATE)
            : fieldSpec(type, name, PRIVATE, FINAL);
      });
    }

    @Override
    public final <R> R acceptRegular(DtoRegularGoal.RegularGoalContextCases<R> cases) {
      return cases.methodGoal(this);
    }

    @Override
    public final <R> R accept(DtoGoalContext.GoalCases<R> cases) {
      return cases.regularGoal(this);
    }

    @Override
    public final List<String> parameterNames() {
      return details.parameterNames;
    }

    @Override
    public final TypeName type() {
      return details.goalType;
    }

    public abstract <R> R acceptMethod(MethodGoalCases<R> cases);
  }

  private static final Function<AbstractMethodGoalContext, List<AbstractRegularStep>> steps =
      methodGoalCases(
          simple -> unmodifiableList(simple.steps),
          projected -> unmodifiableList(projected.steps));

  static final class SimpleMethodGoalContext extends AbstractMethodGoalContext {
    final List<SimpleRegularStep> steps;

    SimpleMethodGoalContext(
        BuildersContext context,
        MethodGoalDetails details,
        List<SimpleRegularStep> steps,
        List<TypeName> thrownTypes) {
      super(context, details, thrownTypes);
      this.steps = steps;
    }

    @Override
    public <R> R acceptMethod(MethodGoalCases<R> cases) {
      return cases.simple(this);
    }
  }

  static final class ProjectedMethodGoalContext extends AbstractMethodGoalContext {
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
    public <R> R acceptMethod(MethodGoalCases<R> cases) {
      return cases.projected(this);
    }
  }

  private DtoMethodGoal() {
    throw new UnsupportedOperationException("no instances");
  }
}
