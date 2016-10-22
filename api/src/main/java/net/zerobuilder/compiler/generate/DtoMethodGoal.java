package net.zerobuilder.compiler.generate;

import com.squareup.javapoet.ClassName;
import com.squareup.javapoet.FieldSpec;
import com.squareup.javapoet.TypeName;

import java.util.List;
import java.util.function.Supplier;

import static javax.lang.model.element.Modifier.FINAL;
import static javax.lang.model.element.Modifier.PRIVATE;
import static net.zerobuilder.compiler.generate.DtoContext.BuilderLifecycle.REUSE_INSTANCES;
import static net.zerobuilder.compiler.generate.Utilities.downcase;
import static net.zerobuilder.compiler.generate.Utilities.fieldSpec;
import static net.zerobuilder.compiler.generate.Utilities.memoize;

public class DtoMethodGoal {

  static final class MethodGoalContext
      extends DtoRegularGoal.AbstractRegularGoalContext {

    final DtoContext.BuildersContext context;
    final DtoIMethodGoal.MethodGoal goal;

    DtoGoal.GoalMethodType methodType() {
      return goal.details.methodType;
    }

    private final Supplier<FieldSpec> field;

    /**
     * @return An instance of type {@link DtoContext.BuildersContext#type}.
     */
    FieldSpec field() {
      return field.get();
    }

    MethodGoalContext(DtoIMethodGoal.MethodGoal goal,
                      DtoContext.BuildersContext context) {
      this.goal = goal;
      this.context = context;
      this.field = memoizeField(context);
    }

    private static Supplier<FieldSpec> memoizeField(DtoContext.BuildersContext context) {
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
      return goal.details.parameterNames;
    }

    @Override
    public TypeName type() {
      return goal.details.goalType;
    }
  }

  private DtoMethodGoal() {
    throw new UnsupportedOperationException("no instances");
  }
}
