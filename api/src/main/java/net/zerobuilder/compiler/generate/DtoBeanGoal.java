package net.zerobuilder.compiler.generate;

import com.squareup.javapoet.ClassName;
import com.squareup.javapoet.FieldSpec;
import net.zerobuilder.compiler.generate.DtoBeanGoalDescription.BeanGoalDescription;
import net.zerobuilder.compiler.generate.DtoGoalDetails.BeanGoalDetails;

import static javax.lang.model.element.Modifier.FINAL;
import static javax.lang.model.element.Modifier.PRIVATE;
import static net.zerobuilder.compiler.generate.ZeroUtil.downcase;
import static net.zerobuilder.compiler.generate.ZeroUtil.fieldSpec;

public final class DtoBeanGoal {

  public static final class BeanGoalContext {

    public final BeanGoalDetails details;
    public final BeanGoalDescription description;

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

    BeanGoalContext(BeanGoalDetails details,
                    BeanGoalDescription description) {
      this.description = description;
      this.bean = beanSupplier(details.goalType);
      this.details = details;
    }

    private static FieldSpec beanSupplier(ClassName type) {
      String name = downcase(type.simpleName());
      return fieldSpec(type, name, PRIVATE, FINAL);
    }

    public ClassName type() {
      return details.goalType;
    }
  }

  private DtoBeanGoal() {
    throw new UnsupportedOperationException("no instances");
  }
}
