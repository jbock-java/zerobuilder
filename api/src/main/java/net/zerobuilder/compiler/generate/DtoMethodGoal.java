package net.zerobuilder.compiler.generate;

import com.squareup.javapoet.ClassName;
import com.squareup.javapoet.FieldSpec;
import com.squareup.javapoet.TypeName;
import net.zerobuilder.compiler.generate.DtoContext.BuildersContext;
import net.zerobuilder.compiler.generate.DtoGoalDetails.InstanceMethodGoalDetails;
import net.zerobuilder.compiler.generate.DtoGoalDetails.StaticMethodGoalDetails;
import net.zerobuilder.compiler.generate.DtoRegularGoal.SimpleRegularGoalContext;

import java.util.List;
import java.util.function.Supplier;

import static javax.lang.model.element.Modifier.FINAL;
import static javax.lang.model.element.Modifier.PRIVATE;
import static net.zerobuilder.compiler.generate.DtoContext.BuilderLifecycle.REUSE_INSTANCES;
import static net.zerobuilder.compiler.generate.DtoRegularStep.SimpleRegularStep;
import static net.zerobuilder.compiler.generate.ZeroUtil.downcase;
import static net.zerobuilder.compiler.generate.ZeroUtil.fieldSpec;
import static net.zerobuilder.compiler.generate.ZeroUtil.memoize;

public final class DtoMethodGoal {

  public static final class InstanceMethodGoalContext extends SimpleRegularGoalContext {
    public final List<SimpleRegularStep> steps;

    InstanceMethodGoalContext(
        BuildersContext context,
        InstanceMethodGoalDetails details,
        List<SimpleRegularStep> steps,
        List<TypeName> thrownTypes) {
      super(thrownTypes);
      this.details = details;
      this.context = context;
      this.instanceField = memoizeInstanceField(context);
      this.steps = steps;
    }

    public final BuildersContext context;
    public final InstanceMethodGoalDetails details;

    private final Supplier<FieldSpec> instanceField;

    /**
     * @return An instance of type {@link BuildersContext#type}.
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
        BuildersContext context,
        StaticMethodGoalDetails details,
        List<SimpleRegularStep> steps,
        List<TypeName> thrownTypes) {
      super(thrownTypes);
      this.details = details;
      this.context = context;
      this.steps = steps;
    }

    public final BuildersContext context;
    public final StaticMethodGoalDetails details;

    List<SimpleRegularStep> methodSteps() {
      return steps;
    }


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

  private static Supplier<FieldSpec> memoizeInstanceField(BuildersContext context) {
    return memoize(() -> {
      ClassName type = context.type;
      String name = '_' + downcase(type.simpleName());
      return context.lifecycle == REUSE_INSTANCES
          ? fieldSpec(type, name, PRIVATE)
          : fieldSpec(type, name, PRIVATE, FINAL);
    });
  }

  private DtoMethodGoal() {
    throw new UnsupportedOperationException("no instances");
  }
}
