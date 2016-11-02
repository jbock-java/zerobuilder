package net.zerobuilder.compiler.generate;

import com.squareup.javapoet.ClassName;
import com.squareup.javapoet.FieldSpec;
import com.squareup.javapoet.TypeName;
import net.zerobuilder.compiler.generate.DtoBeanStep.AbstractBeanStep;
import net.zerobuilder.compiler.generate.DtoContext.BuildersContext;
import net.zerobuilder.compiler.generate.DtoGoalDetails.BeanGoalDetails;
import net.zerobuilder.compiler.generate.DtoGoalContext.AbstractGoalContext;
import net.zerobuilder.compiler.generate.DtoGoalContext.GoalCases;
import net.zerobuilder.compiler.generate.DtoProjectedGoal.ProjectedGoal;
import net.zerobuilder.compiler.generate.DtoProjectedGoal.ProjectedGoalCases;
import net.zerobuilder.compiler.generate.DtoSimpleGoal.SimpleGoal;

import java.util.List;
import java.util.function.Supplier;

import static javax.lang.model.element.Modifier.FINAL;
import static javax.lang.model.element.Modifier.PRIVATE;
import static net.zerobuilder.compiler.generate.DtoContext.BuilderLifecycle.REUSE_INSTANCES;
import static net.zerobuilder.compiler.generate.ZeroUtil.downcase;
import static net.zerobuilder.compiler.generate.ZeroUtil.fieldSpec;
import static net.zerobuilder.compiler.generate.ZeroUtil.memoize;

public final class DtoBeanGoal {

  public static final class BeanGoalContext extends AbstractGoalContext
      implements ProjectedGoal, SimpleGoal {

    public final BuildersContext context;
    public final List<AbstractBeanStep> steps;
    public final BeanGoalDetails details;
    public final List<TypeName> thrownTypes;

    private final Supplier<FieldSpec> bean;

    /**
     * A instanceField that holds an instance of the bean type.
     *
     * @return instanceField spec
     */
    public FieldSpec bean() {
      return bean.get();
    }

    BeanGoalContext(BuildersContext context,
                    BeanGoalDetails details,
                    List<AbstractBeanStep> steps,
                    List<TypeName> thrownTypes) {
      this.context = context;
      this.bean = beanSupplier(details.goalType, context);
      this.steps = steps;
      this.details = details;
      this.thrownTypes = thrownTypes;
    }

    private static Supplier<FieldSpec> beanSupplier(ClassName type, BuildersContext context) {
      return memoize(() -> {
        String name = downcase(type.simpleName());
        return context.lifecycle == REUSE_INSTANCES
            ? fieldSpec(type, name, PRIVATE)
            : fieldSpec(type, name, PRIVATE, FINAL);
      });
    }

    public ClassName type() {
      return details.goalType;
    }

    <R> R accept(GoalCases<R> cases) {
      return cases.beanGoal(this);
    }

    @Override
    public <R> R acceptProjected(ProjectedGoalCases<R> cases) {
      return cases.bean(this);
    }

    @Override
    public <R> R acceptSimple(DtoSimpleGoal.SimpleGoalCases<R> cases) {
      return cases.bean(this);
    }
  }

  private DtoBeanGoal() {
    throw new UnsupportedOperationException("no instances");
  }
}
