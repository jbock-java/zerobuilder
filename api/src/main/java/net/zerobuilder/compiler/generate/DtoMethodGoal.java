package net.zerobuilder.compiler.generate;

import com.squareup.javapoet.ClassName;
import com.squareup.javapoet.CodeBlock;
import com.squareup.javapoet.FieldSpec;
import com.squareup.javapoet.TypeName;
import net.zerobuilder.compiler.generate.DtoContext.BuildersContext;
import net.zerobuilder.compiler.generate.DtoGoal.GoalMethodType;
import net.zerobuilder.compiler.generate.DtoGoal.MethodGoalDetails;
import net.zerobuilder.compiler.generate.DtoProjectedRegularGoalContext.ProjectedMethodGoalContext;
import net.zerobuilder.compiler.generate.DtoRegularGoal.AbstractRegularGoalContext;
import net.zerobuilder.compiler.generate.DtoRegularStep.AbstractRegularStep;

import java.util.List;
import java.util.function.Function;
import java.util.function.Supplier;

import static com.squareup.javapoet.TypeName.VOID;
import static java.util.Collections.unmodifiableList;
import static javax.lang.model.element.Modifier.FINAL;
import static javax.lang.model.element.Modifier.PRIVATE;
import static net.zerobuilder.compiler.generate.DtoContext.BuilderLifecycle.REUSE_INSTANCES;
import static net.zerobuilder.compiler.generate.DtoGoal.GoalMethodType.INSTANCE_METHOD;
import static net.zerobuilder.compiler.generate.DtoRegularStep.SimpleRegularStep;
import static net.zerobuilder.compiler.generate.Utilities.downcase;
import static net.zerobuilder.compiler.generate.Utilities.emptyCodeBlock;
import static net.zerobuilder.compiler.generate.Utilities.fieldSpec;
import static net.zerobuilder.compiler.generate.Utilities.memoize;
import static net.zerobuilder.compiler.generate.Utilities.statement;

public final class DtoMethodGoal {

  static final class SimpleMethodGoalContext extends AbstractRegularGoalContext {
    final List<SimpleRegularStep> steps;

    SimpleMethodGoalContext(
        BuildersContext context,
        MethodGoalDetails details,
        List<SimpleRegularStep> steps,
        List<TypeName> thrownTypes) {
      this.details = details;
      this.thrownTypes = thrownTypes;
      this.context = context;
      this.field = memoizeField(context);
      this.steps = steps;
    }

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

    List<SimpleRegularStep> methodSteps() {
      return steps;
    }

    CodeBlock methodGoalInvocation() {
      TypeName type = type();
      String method = details.methodName;
      return details.methodType == INSTANCE_METHOD ?
          statement("return this.$N.$N($L)", field(), method, invocationParameters()) :
          CodeBlock.builder()
              .add(VOID.equals(type) ? emptyCodeBlock : CodeBlock.of("return "))
              .addStatement("$T.$N($L)", context.type, method, invocationParameters())
              .build();
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
