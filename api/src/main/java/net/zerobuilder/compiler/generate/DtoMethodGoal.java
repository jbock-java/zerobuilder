package net.zerobuilder.compiler.generate;

import com.squareup.javapoet.FieldSpec;
import com.squareup.javapoet.TypeName;
import net.zerobuilder.compiler.generate.DtoContext.GoalContext;
import net.zerobuilder.compiler.generate.DtoGoalDetails.InstanceMethodGoalDetails;
import net.zerobuilder.compiler.generate.DtoGoalDetails.StaticMethodGoalDetails;
import net.zerobuilder.compiler.generate.DtoRegularGoal.SimpleRegularGoalContext;
import net.zerobuilder.compiler.generate.DtoRegularParameter.SimpleParameter;

import java.util.List;
import java.util.function.Supplier;

import static javax.lang.model.element.Modifier.FINAL;
import static javax.lang.model.element.Modifier.PRIVATE;
import static net.zerobuilder.compiler.generate.DtoContext.ContextLifecycle.REUSE_INSTANCES;
import static net.zerobuilder.compiler.generate.DtoRegularStep.SimpleRegularStep;
import static net.zerobuilder.compiler.generate.ZeroUtil.applyRanking;
import static net.zerobuilder.compiler.generate.ZeroUtil.createRanking;
import static net.zerobuilder.compiler.generate.ZeroUtil.downcase;
import static net.zerobuilder.compiler.generate.ZeroUtil.fieldSpec;
import static net.zerobuilder.compiler.generate.ZeroUtil.memoize;
import static net.zerobuilder.compiler.generate.ZeroUtil.rawClassName;

public final class DtoMethodGoal {

  public static final class InstanceMethodGoalContext extends SimpleRegularGoalContext {
    public final List<SimpleRegularStep> steps;

    InstanceMethodGoalContext(
        DtoContext.GoalContext context,
        InstanceMethodGoalDetails details,
        List<SimpleRegularStep> steps,
        List<TypeName> thrownTypes) {
      super(thrownTypes);
      this.details = details;
      this.context = context;
      this.instanceField = memoizeInstanceField(context);
      this.steps = steps;
    }

    public final GoalContext context;
    public final InstanceMethodGoalDetails details;

    private final Supplier<FieldSpec> instanceField;

    /**
     * @return An instance of type {@link DtoContext.GoalContext#type}.
     */
    public FieldSpec instanceField() {
      return instanceField.get();
    }

    List<SimpleRegularStep> methodSteps() {
      return steps;
    }

    @Override
    public final <R> R acceptRegular(DtoRegularGoal.RegularGoalContextCases<R> cases) {
      return cases.instanceMethod(this);
    }

    @Override
    public final List<String> parameterNames() {
      return details.parameterNames;
    }

    @Override
    public final TypeName type() {
      return details.goalType;
    }
  }

  public static final class SimpleStaticMethodGoalContext extends SimpleRegularGoalContext {

    public final List<SimpleRegularStep> steps;

    SimpleStaticMethodGoalContext(
        GoalContext context,
        StaticMethodGoalDetails details,
        List<SimpleRegularStep> steps,
        List<TypeName> thrownTypes, List<SimpleParameter> parameters) {
      super(thrownTypes);
      this.details = details;
      this.context = context;
      this.steps = steps;
      this.parameters = parameters;
    }

    private static int[] createUnshuffle(List<SimpleParameter> parameters, List<String> parameterNames) {
      String[] a = new String[parameters.size()];
      for (int i = 0; i < parameters.size(); i++) {
        a[i] = parameters.get(i).name;
      }
      String[] b = parameterNames.toArray(new String[parameterNames.size()]);
      return createRanking(a, b);
    }

    public <E> List<E> unshuffle(List<E> shuffled) {
      int[] unshuffle = createUnshuffle(parameters, details.parameterNames);
      return applyRanking(unshuffle, shuffled);
    }

    public final DtoContext.GoalContext context;
    public final StaticMethodGoalDetails details;
    public final List<SimpleParameter> parameters;

    @Override
    public final <R> R acceptRegular(DtoRegularGoal.RegularGoalContextCases<R> cases) {
      return cases.staticMethod(this);
    }

    @Override
    public final List<String> parameterNames() {
      return details.parameterNames;
    }

    @Override
    public final TypeName type() {
      return details.goalType;
    }
  }

  private static Supplier<FieldSpec> memoizeInstanceField(GoalContext context) {
    return memoize(() -> {
      TypeName type = context.type;
      String name = '_' + downcase(rawClassName(type).get().simpleName());
      return context.lifecycle == REUSE_INSTANCES
          ? fieldSpec(type, name, PRIVATE)
          : fieldSpec(type, name, PRIVATE, FINAL);
    });
  }

  private DtoMethodGoal() {
    throw new UnsupportedOperationException("no instances");
  }
}
