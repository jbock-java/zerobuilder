package net.zerobuilder.compiler.generate;

import com.squareup.javapoet.ClassName;
import com.squareup.javapoet.FieldSpec;
import com.squareup.javapoet.TypeName;
import net.zerobuilder.compiler.generate.DtoBeanParameter.AbstractBeanParameter;
import net.zerobuilder.compiler.generate.DtoGoalDetails.BeanGoalDetails;

import java.util.List;

import static javax.lang.model.element.Modifier.FINAL;
import static javax.lang.model.element.Modifier.PRIVATE;
import static net.zerobuilder.compiler.generate.ZeroUtil.downcase;
import static net.zerobuilder.compiler.generate.ZeroUtil.fieldSpec;

public final class DtoBeanGoalDescription {

  /**
   * Describes the task of creating and / or updating a JavaBean.
   */
  public static final class BeanGoalDescription {

    public final BeanGoalDetails details;
    public final List<AbstractBeanParameter> parameters;
    public final FieldSpec beanField;
    
    // thrown by constructor
    public final List<TypeName> thrownTypes;

    private BeanGoalDescription(BeanGoalDetails details,
                                List<AbstractBeanParameter> parameters,
                                List<TypeName> thrownTypes) {
      this.details = details;
      this.parameters = parameters;
      this.thrownTypes = thrownTypes;
      this.beanField = beanField(details.goalType);
    }

    private static FieldSpec beanField(ClassName type) {
      String name = downcase(type.simpleName());
      return fieldSpec(type, name, PRIVATE, FINAL);
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
