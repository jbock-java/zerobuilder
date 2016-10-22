package net.zerobuilder.compiler.generate;

import com.squareup.javapoet.ClassName;
import com.squareup.javapoet.FieldSpec;
import com.squareup.javapoet.TypeName;
import net.zerobuilder.compiler.generate.DtoContext.BuildersContext;
import net.zerobuilder.compiler.generate.DtoGoal.MethodGoalDetails;
import net.zerobuilder.compiler.generate.DtoRegularGoal.AbstractRegularGoalContext;

import java.util.List;
import java.util.function.Supplier;

import static javax.lang.model.element.Modifier.FINAL;
import static javax.lang.model.element.Modifier.PRIVATE;
import static net.zerobuilder.compiler.generate.DtoContext.BuilderLifecycle.REUSE_INSTANCES;
import static net.zerobuilder.compiler.generate.Utilities.downcase;
import static net.zerobuilder.compiler.generate.Utilities.fieldSpec;
import static net.zerobuilder.compiler.generate.Utilities.memoize;

public class DtoMethodGoal {

  static final class MethodGoalContext extends AbstractRegularGoalContext implements DtoGoalContext.IGoal {

    final BuildersContext context;
    final MethodGoalDetails details;
    final List<DtoRegularStep.AbstractRegularStep> steps;
    final List<TypeName> thrownTypes;

    DtoGoal.GoalMethodType methodType() {
      return details.methodType;
    }

    private final Supplier<FieldSpec> field;

    /**
     * @return An instance of type {@link BuildersContext#type}.
     */
    FieldSpec field() {
      return field.get();
    }

    MethodGoalContext(BuildersContext context,
                      MethodGoalDetails details,
                      List<DtoRegularStep.AbstractRegularStep> steps,
                      List<TypeName> thrownTypes) {
      this.details = details;
      this.steps = steps;
      this.thrownTypes = thrownTypes;
      this.context = context;
      this.field = memoizeField(context);
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
    public <R> R acceptRegular(DtoRegularGoal.RegularGoalContextCases<R> cases) {
      return cases.methodGoal(this);
    }

    @Override
    public <R> R accept(DtoGoalContext.GoalCases<R> cases) {
      return cases.regularGoal(this);
    }

    @Override
    public List<String> parameterNames() {
      return details.parameterNames;
    }

    @Override
    public TypeName type() {
      return details.goalType;
    }

    @Override
    public DtoGoalContext.AbstractGoalContext withContext(BuildersContext context) {
      return this;
    }
  }

  private DtoMethodGoal() {
    throw new UnsupportedOperationException("no instances");
  }
}
