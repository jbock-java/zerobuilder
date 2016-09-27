package net.zerobuilder.compiler.generate;

import com.google.common.collect.ImmutableList;
import com.squareup.javapoet.ClassName;
import com.squareup.javapoet.FieldSpec;
import net.zerobuilder.compiler.analyse.DtoGoal.BeanGoal;
import net.zerobuilder.compiler.generate.DtoBeanStep.AbstractBeanStep;
import net.zerobuilder.compiler.generate.DtoBuilders.BuildersContext;
import net.zerobuilder.compiler.generate.DtoGoalContext.AbstractGoalContext;
import net.zerobuilder.compiler.generate.DtoGoalContext.GoalCases;

public final class DtoBeanGoalContext {

  public final static class BeanGoalContext extends AbstractGoalContext {

    /**
     * alphabetic order unless {@link net.zerobuilder.Step} was used
     */
    final ImmutableList<? extends AbstractBeanStep> steps;
    final BeanGoal goal;
    final FieldSpec field;

    public BeanGoalContext(BeanGoal goal,
                           BuildersContext builders,
                           boolean toBuilder,
                           boolean builder,
                           ClassName contractName,
                           ImmutableList<? extends AbstractBeanStep> steps, FieldSpec field) {
      super(builders, toBuilder, builder, contractName);
      this.steps = steps;
      this.goal = goal;
      this.field = field;
    }

    <R> R accept(GoalCases<R> cases) {
      return cases.beanGoal(this);
    }
  }

  private DtoBeanGoalContext() {
    throw new UnsupportedOperationException("no instances");
  }
}
