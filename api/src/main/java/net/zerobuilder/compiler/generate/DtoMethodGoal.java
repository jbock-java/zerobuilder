package net.zerobuilder.compiler.generate;

import com.squareup.javapoet.FieldSpec;
import com.squareup.javapoet.TypeName;
import net.zerobuilder.compiler.generate.DtoContext.ContextLifecycle;
import net.zerobuilder.compiler.generate.DtoContext.GoalContext;
import net.zerobuilder.compiler.generate.DtoGoalDetails.InstanceMethodGoalDetails;
import net.zerobuilder.compiler.generate.DtoGoalDetails.StaticMethodGoalDetails;
import net.zerobuilder.compiler.generate.DtoRegularGoal.SimpleRegularGoalContext;
import net.zerobuilder.compiler.generate.DtoRegularGoalDescription.SimpleRegularGoalDescription;

import java.util.function.Supplier;

import static javax.lang.model.element.Modifier.FINAL;
import static javax.lang.model.element.Modifier.PRIVATE;
import static net.zerobuilder.compiler.generate.DtoContext.ContextLifecycle.REUSE_INSTANCES;
import static net.zerobuilder.compiler.generate.ZeroUtil.downcase;
import static net.zerobuilder.compiler.generate.ZeroUtil.fieldSpec;
import static net.zerobuilder.compiler.generate.ZeroUtil.memoize;
import static net.zerobuilder.compiler.generate.ZeroUtil.simpleName;

public final class DtoMethodGoal {

  public static final class InstanceMethodGoalContext extends SimpleRegularGoalContext {

    public InstanceMethodGoalContext(
        GoalContext context,
        InstanceMethodGoalDetails details,
        SimpleRegularGoalDescription description) {
      super(description, createUnshuffle(description.parameters(), details.parameterNames));
      this.details = details;
      this.context = context;
      this.instanceField = memoizeInstanceField(context, details.lifecycle);
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

    @Override
    public final <R> R acceptRegular(DtoRegularGoal.RegularGoalContextCases<R> cases) {
      return cases.instanceMethod(this);
    }

    @Override
    public final TypeName type() {
      return details.goalType;
    }
  }

  public static final class SimpleStaticMethodGoalContext extends SimpleRegularGoalContext {

    public SimpleStaticMethodGoalContext(
        GoalContext context,
        StaticMethodGoalDetails details,
        SimpleRegularGoalDescription description) {
      super(description, createUnshuffle(description.parameters(), details.parameterNames));
      this.details = details;
      this.context = context;
    }

    public final DtoContext.GoalContext context;
    public final StaticMethodGoalDetails details;

    @Override
    public final <R> R acceptRegular(DtoRegularGoal.RegularGoalContextCases<R> cases) {
      return cases.staticMethod(this);
    }

    @Override
    public final TypeName type() {
      return details.goalType;
    }
  }

  private static Supplier<FieldSpec> memoizeInstanceField(GoalContext context, ContextLifecycle lifecycle) {
    return memoize(() -> {
      TypeName type = context.type;
      String name = '_' + downcase(simpleName(type));
      return lifecycle == REUSE_INSTANCES
          ? fieldSpec(type, name, PRIVATE)
          : fieldSpec(type, name, PRIVATE, FINAL);
    });
  }

  private DtoMethodGoal() {
    throw new UnsupportedOperationException("no instances");
  }
}
