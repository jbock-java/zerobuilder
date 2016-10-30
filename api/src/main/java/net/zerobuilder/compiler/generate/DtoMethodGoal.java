package net.zerobuilder.compiler.generate;

import com.squareup.javapoet.ClassName;
import com.squareup.javapoet.CodeBlock;
import com.squareup.javapoet.FieldSpec;
import com.squareup.javapoet.TypeName;
import net.zerobuilder.compiler.generate.DtoContext.BuildersContext;
import net.zerobuilder.compiler.generate.DtoGoalDetails.InstanceMethodGoalDetails;
import net.zerobuilder.compiler.generate.DtoGoalDetails.StaticMethodGoalDetails;
import net.zerobuilder.compiler.generate.DtoRegularGoal.SimpleRegularGoalContext;

import java.util.List;
import java.util.function.Supplier;

import static com.squareup.javapoet.TypeName.VOID;
import static javax.lang.model.element.Modifier.FINAL;
import static javax.lang.model.element.Modifier.PRIVATE;
import static net.zerobuilder.compiler.generate.DtoContext.BuilderLifecycle.REUSE_INSTANCES;
import static net.zerobuilder.compiler.generate.DtoRegularStep.SimpleRegularStep;
import static net.zerobuilder.compiler.generate.ZeroUtil.downcase;
import static net.zerobuilder.compiler.generate.ZeroUtil.emptyCodeBlock;
import static net.zerobuilder.compiler.generate.ZeroUtil.fieldSpec;
import static net.zerobuilder.compiler.generate.ZeroUtil.memoize;
import static net.zerobuilder.compiler.generate.ZeroUtil.statement;

public final class DtoMethodGoal {

  static final class InstanceMethodGoalContext extends SimpleRegularGoalContext {
    final List<SimpleRegularStep> steps;

    InstanceMethodGoalContext(
        BuildersContext context,
        InstanceMethodGoalDetails details,
        List<SimpleRegularStep> steps,
        List<TypeName> thrownTypes) {
      super(thrownTypes);
      this.details = details;
      this.context = context;
      this.field = memoizeField(context);
      this.steps = steps;
    }

    final BuildersContext context;
    final InstanceMethodGoalDetails details;

    private final Supplier<FieldSpec> field;

    /**
     * @return An instance of type {@link BuildersContext#type}.
     */
    FieldSpec field() {
      return field.get();
    }

    List<SimpleRegularStep> methodSteps() {
      return steps;
    }

    CodeBlock methodGoalInvocation() {
      String method = details.methodName;
      return statement("return this.$N.$N($L)", field(), method, invocationParameters());
    }

    @Override
    public final <R> R acceptRegular(DtoRegularGoal.RegularGoalContextCases<R> cases) {
      return cases.methodGoal(this);
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

  static final class SimpleStaticMethodGoalContext extends SimpleRegularGoalContext {
    final List<SimpleRegularStep> steps;

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

    final BuildersContext context;
    final StaticMethodGoalDetails details;

    List<SimpleRegularStep> methodSteps() {
      return steps;
    }

    CodeBlock methodGoalInvocation() {
      TypeName type = type();
      String method = details.methodName;
      return CodeBlock.builder()
          .add(VOID.equals(type) ? emptyCodeBlock : CodeBlock.of("return "))
          .addStatement("$T.$N($L)", context.type, method, invocationParameters())
          .build();
    }

    @Override
    public final <R> R acceptRegular(DtoRegularGoal.RegularGoalContextCases<R> cases) {
      return cases.staticMethodGoal(this);
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

  private static Supplier<FieldSpec> memoizeField(BuildersContext context) {
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
