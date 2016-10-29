package net.zerobuilder.compiler.generate;

import com.squareup.javapoet.TypeName;
import net.zerobuilder.compiler.generate.DtoBeanParameter.AbstractBeanParameter;
import net.zerobuilder.compiler.generate.DtoGoalDetails.BeanGoalDetails;
import net.zerobuilder.compiler.generate.DtoProjectedDescription.ProjectedDescription;
import net.zerobuilder.compiler.generate.DtoProjectedDescription.ProjectedDescriptionCases;
import net.zerobuilder.compiler.generate.DtoSimpleDescription.SimpleDescription;

import java.util.List;

public final class DtoBeanGoalDescription {

  /**
   * Describes the task of creating and / or updating a JavaBean.
   */
  public static final class BeanGoalDescription
      implements ProjectedDescription, SimpleDescription {

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
    public <R> R acceptProjected(ProjectedDescriptionCases<R> cases) {
      return cases.bean(this);
    }

    @Override
    public <R> R acceptSimple(DtoSimpleDescription.SimpleDescriptionCases<R> cases) {
      return cases.bean(this);
    }
  }

  private DtoBeanGoalDescription() {
    throw new UnsupportedOperationException("no instances");
  }
}
