package net.zerobuilder.compiler.generate;

import com.google.common.collect.ImmutableList;
import com.squareup.javapoet.FieldSpec;
import net.zerobuilder.compiler.generate.DtoGoal.BeanGoalDetails;
import net.zerobuilder.compiler.generate.DtoBeanStep.AbstractBeanStep;
import net.zerobuilder.compiler.generate.DtoBuilders.BuildersContext;
import net.zerobuilder.compiler.generate.DtoGoalContext.AbstractGoalContext;
import net.zerobuilder.compiler.generate.DtoGoalContext.GoalCases;
import net.zerobuilder.compiler.generate.DtoGoalContext.IGoal;

import java.util.List;

import static javax.lang.model.element.Modifier.PRIVATE;
import static net.zerobuilder.compiler.Utilities.downcase;
import static net.zerobuilder.compiler.Utilities.fieldSpec;

public final class DtoBeanGoalContext {

  public static final class BeanGoal implements IGoal {

    /**
     * alphabetic order unless {@link net.zerobuilder.Step} was used
     */
    final ImmutableList<? extends AbstractBeanStep> steps;
    final BeanGoalDetails details;
    final FieldSpec field;

    private BeanGoal(BeanGoalDetails details,
                     ImmutableList<? extends AbstractBeanStep> steps, FieldSpec field) {
      this.steps = steps;
      this.details = details;
      this.field = field;
    }

    public static BeanGoal create(BeanGoalDetails goal,
                                  List<? extends AbstractBeanStep> steps) {
      FieldSpec field = fieldSpec(goal.goalType,
          downcase(goal.goalType.simpleName()), PRIVATE);
      return new BeanGoal(goal, ImmutableList.copyOf(steps), field);

    }

    @Override
    public AbstractGoalContext withContext(BuildersContext context) {
      return new BeanGoalContext(this, context);
    }
  }

  static final class BeanGoalContext implements AbstractGoalContext {

    final BuildersContext builders;
    final BeanGoal goal;

    BeanGoalContext(BeanGoal goal,
                           BuildersContext builders) {
      this.goal = goal;
      this.builders = builders;
    }

    public <R> R accept(GoalCases<R> cases) {
      return cases.beanGoal(this);
    }
  }

  private DtoBeanGoalContext() {
    throw new UnsupportedOperationException("no instances");
  }
}
