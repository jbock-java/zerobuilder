package net.zerobuilder.compiler.generate;

import com.squareup.javapoet.ClassName;
import com.squareup.javapoet.FieldSpec;
import net.zerobuilder.compiler.generate.DtoBeanGoalDescription.BeanGoalDescription;
import net.zerobuilder.compiler.generate.DtoContext.GoalContext;
import net.zerobuilder.compiler.generate.DtoGoalDetails.BeanGoalDetails;

import static javax.lang.model.element.Modifier.FINAL;
import static javax.lang.model.element.Modifier.PRIVATE;
import static net.zerobuilder.compiler.generate.DtoContext.ContextLifecycle.REUSE_INSTANCES;
import static net.zerobuilder.compiler.generate.ZeroUtil.downcase;
import static net.zerobuilder.compiler.generate.ZeroUtil.fieldSpec;

public final class DtoBeanGoal {

  public static final class BeanGoalContext {

    public final GoalContext context;
    public final BeanGoalDetails details;
    private final BeanGoalDescription description;

    public BeanGoalDescription description() {
      return description;
    }

    private final FieldSpec bean;

    public final Boolean mayReuse() {
      return context.lifecycle == REUSE_INSTANCES;
    }

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
  }

  private DtoBeanGoal() {
    throw new UnsupportedOperationException("no instances");
  }
}
