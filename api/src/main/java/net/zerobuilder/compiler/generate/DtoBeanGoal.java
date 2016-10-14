package net.zerobuilder.compiler.generate;

import com.squareup.javapoet.ClassName;
import com.squareup.javapoet.FieldSpec;
import com.squareup.javapoet.TypeName;
import net.zerobuilder.compiler.generate.DtoBeanStep.AbstractBeanStep;
import net.zerobuilder.compiler.generate.DtoContext.BuildersContext;
import net.zerobuilder.compiler.generate.DtoGoal.BeanGoalDetails;
import net.zerobuilder.compiler.generate.DtoGoalContext.AbstractGoalContext;
import net.zerobuilder.compiler.generate.DtoGoalContext.GoalCases;
import net.zerobuilder.compiler.generate.DtoGoalContext.IGoal;

import java.util.List;
import java.util.function.Supplier;

import static java.util.Collections.unmodifiableList;
import static javax.lang.model.element.Modifier.FINAL;
import static javax.lang.model.element.Modifier.PRIVATE;
import static net.zerobuilder.compiler.generate.DtoContext.BuilderLifecycle.REUSE_INSTANCES;
import static net.zerobuilder.compiler.generate.Utilities.downcase;
import static net.zerobuilder.compiler.generate.Utilities.fieldSpec;
import static net.zerobuilder.compiler.generate.Utilities.memoize;

final class DtoBeanGoal {

  static final class BeanGoal implements IGoal {

    private final List<AbstractBeanStep> steps;
    final BeanGoalDetails details;
    final List<TypeName> thrownTypes;

    private BeanGoal(BeanGoalDetails details,
                     List<? extends AbstractBeanStep> steps, List<TypeName> thrownTypes) {
      this.steps = unmodifiableList(steps);
      this.details = details;
      this.thrownTypes = thrownTypes;
    }

    static BeanGoal create(BeanGoalDetails details,
                           List<? extends AbstractBeanStep> steps,
                           List<TypeName> thrownTypes) {
      return new BeanGoal(details, steps, thrownTypes);
    }

    @Override
    public AbstractGoalContext withContext(BuildersContext context) {
      return new BeanGoalContext(this, context);
    }
  }

  static final class BeanGoalContext extends AbstractGoalContext {

    final BuildersContext context;
    final BeanGoal goal;

    private final Supplier<FieldSpec> bean;

    List<AbstractBeanStep> steps() {
      return goal.steps;
    }

    /**
     * A field that holds an instance of the bean type.
     *
     * @return field spec
     */
    FieldSpec bean() {
      return bean.get();
    }

    BeanGoalContext(BeanGoal goal,
                    BuildersContext context) {
      this.goal = goal;
      this.context = context;
      this.bean = beanSupplier(goal, context);
    }

    private static Supplier<FieldSpec> beanSupplier(BeanGoal goal, BuildersContext context) {
      return memoize(() -> {
        ClassName type = goal.details.goalType;
        String name = downcase(type.simpleName());
        return context.lifecycle == REUSE_INSTANCES
            ? fieldSpec(type, name, PRIVATE)
            : fieldSpec(type, name, PRIVATE, FINAL);
      });
    }

    ClassName type() {
      return goal.details.goalType;
    }

    public <R> R accept(GoalCases<R> cases) {
      return cases.beanGoal(this);
    }
  }

  private DtoBeanGoal() {
    throw new UnsupportedOperationException("no instances");
  }
}
