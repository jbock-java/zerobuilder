package net.zerobuilder.compiler.generate;

import com.squareup.javapoet.TypeName;
import net.zerobuilder.compiler.generate.DtoBeanParameter.AbstractBeanParameter;
import net.zerobuilder.compiler.generate.DtoGoal.BeanGoalDetails;
import net.zerobuilder.compiler.generate.DtoGoalDescription.GoalDescription;
import net.zerobuilder.compiler.generate.DtoGoalDescription.GoalDescriptionCases;

import java.util.List;

public final class DtoBeanGoalDescription {

  /**
   * Describes the task of creating and / or updating a JavaBean.
   */
  public static final class BeanGoalDescription extends GoalDescription {
    final BeanGoalDetails details;
    final List<AbstractBeanParameter> parameters;
    final List<TypeName> thrownTypes;

    private BeanGoalDescription(BeanGoalDetails details,
                                List<AbstractBeanParameter> parameters,
                                List<TypeName> thrownTypes) {
      this.details = details;
      this.parameters = parameters;
      this.thrownTypes = thrownTypes;
    }

    public static BeanGoalDescription create(BeanGoalDetails details, List<AbstractBeanParameter> parameters,
                                             List<TypeName> thrownTypes) {
      return new BeanGoalDescription(details, parameters, thrownTypes);
    }

    @Override
    public <R> R accept(GoalDescriptionCases<R> cases) {
      return cases.beanGoal(this);
    }
  }

  private DtoBeanGoalDescription() {
    throw new UnsupportedOperationException("no instances");
  }
}
