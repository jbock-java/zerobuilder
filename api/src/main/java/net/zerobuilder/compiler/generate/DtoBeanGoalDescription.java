package net.zerobuilder.compiler.generate;

import com.squareup.javapoet.TypeName;
import net.zerobuilder.compiler.generate.DtoBeanParameter.AbstractBeanParameter;
import net.zerobuilder.compiler.generate.DtoGoalDetails.BeanGoalDetails;

import java.util.List;

public final class DtoBeanGoalDescription {

  /**
   * Describes the task of creating and / or updating a JavaBean.
   */
  public static final class BeanGoalDescription {

    public final BeanGoalDetails details;
    private final List<AbstractBeanParameter> parameters;
    public final List<TypeName> thrownTypes;

    public List<AbstractBeanParameter> parameters() {
      return parameters;
    }

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
  }

  private DtoBeanGoalDescription() {
    throw new UnsupportedOperationException("no instances");
  }
}
