package net.zerobuilder.compiler.generate;

import com.squareup.javapoet.ClassName;
import com.squareup.javapoet.FieldSpec;
import net.zerobuilder.compiler.generate.DtoBeanGoalDescription.BeanGoalDescription;
import net.zerobuilder.compiler.generate.DtoContext.GoalContext;
import net.zerobuilder.compiler.generate.DtoGoalContext.AbstractGoalContext;
import net.zerobuilder.compiler.generate.DtoGoalContext.GoalCases;
import net.zerobuilder.compiler.generate.DtoGoalDetails.BeanGoalDetails;
import net.zerobuilder.compiler.generate.DtoProjectedGoal.ProjectedGoal;
import net.zerobuilder.compiler.generate.DtoProjectedGoal.ProjectedGoalCases;
import net.zerobuilder.compiler.generate.DtoSimpleGoal.SimpleGoal;

import static javax.lang.model.element.Modifier.FINAL;
import static javax.lang.model.element.Modifier.PRIVATE;
import static net.zerobuilder.compiler.generate.DtoContext.ContextLifecycle.REUSE_INSTANCES;
import static net.zerobuilder.compiler.generate.ZeroUtil.downcase;
import static net.zerobuilder.compiler.generate.ZeroUtil.fieldSpec;

public final class DtoBeanGoal {

  public static final class BeanGoalContext extends AbstractGoalContext
      implements ProjectedGoal, SimpleGoal {

    public final GoalContext context;
    public final BeanGoalDetails details;
    private final BeanGoalDescription description;

    public BeanGoalDescription description() {
      return description;
    }

    private final FieldSpec bean;

    /**
     * A instanceField that holds an instance of the bean type.
     *
     * @return instanceField spec
     */
    public FieldSpec bean() {
      return bean;
    }

    BeanGoalContext(GoalContext context,
                    BeanGoalDetails details,
                    BeanGoalDescription description) {
      this.context = context;
      this.description = description;
      this.bean = beanSupplier(details.goalType, context);
      this.details = details;
    }

    private static FieldSpec beanSupplier(ClassName type, DtoContext.GoalContext context) {
      String name = downcase(type.simpleName());
      return context.lifecycle == REUSE_INSTANCES
          ? fieldSpec(type, name, PRIVATE)
          : fieldSpec(type, name, PRIVATE, FINAL);
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
