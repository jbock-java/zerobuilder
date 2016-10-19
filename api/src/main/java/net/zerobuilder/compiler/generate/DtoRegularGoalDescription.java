package net.zerobuilder.compiler.generate;

import com.squareup.javapoet.TypeName;
import net.zerobuilder.compiler.generate.DtoBeanParameter.AbstractBeanParameter;
import net.zerobuilder.compiler.generate.DtoGoal.AbstractGoalDetails;
import net.zerobuilder.compiler.generate.DtoGoal.BeanGoalDetails;
import net.zerobuilder.compiler.generate.DtoGoal.RegularGoalDetails;
import net.zerobuilder.compiler.generate.DtoGoalDescription.GoalDescriptionCases;
import net.zerobuilder.compiler.generate.DtoRegularParameter.AbstractRegularParameter;

import java.util.List;
import java.util.function.Function;

public final class DtoRegularGoalDescription {

  /**
   * Describes of a goal that represents either a static method or an instance method, or a constructor.
   */
  public static final class RegularGoalDescription extends DtoGoalDescription.GoalDescription {
    final RegularGoalDetails details;
    final List<TypeName> thrownTypes;
    final List<AbstractRegularParameter> parameters;

    private RegularGoalDescription(RegularGoalDetails details,
                                   List<TypeName> thrownTypes,
                                   List<AbstractRegularParameter> parameters) {
      this.details = details;
      this.thrownTypes = thrownTypes;
      this.parameters = parameters;
    }

    /**
     * @param details     goal details
     * @param thrownTypes exceptions thrown by the goal method, if any
     * @param parameters  parameter names, possibly in a different order;
     *                    must contain each parameter name in {@link RegularGoalDetails#parameterNames}
     *                    exactly once
     * @return goal description
     * @throws IllegalArgumentException if {@code parameters} don't match the parameter names in {@code details}
     */
    public static RegularGoalDescription create(RegularGoalDetails details,
                                                List<TypeName> thrownTypes,
                                                List<AbstractRegularParameter> parameters) {
      checkParameterNames(details.parameterNames, parameters);
      return new RegularGoalDescription(details, thrownTypes, parameters);
    }

    private static void checkParameterNames(List<String> parameterNames,
                                            List<AbstractRegularParameter> parameters) {
      if (parameters.isEmpty()) {
        throw new IllegalArgumentException("need at least one parameter");
      }
      if (parameterNames.size() != parameters.size()) {
        throw new IllegalArgumentException("parameter names mismatch");
      }
      int[] positions = new int[parameterNames.size()];
      for (AbstractRegularParameter parameter : parameters) {
        int i = parameterNames.indexOf(parameter.name);
        if (positions[i]++ != 0) {
          throw new IllegalArgumentException("parameter names mismatch");
        }
      }
    }

    @Override
    public <R> R accept(GoalDescriptionCases<R> cases) {
      return cases.regularGoal(this);
    }
  }

  private DtoRegularGoalDescription() {
    throw new UnsupportedOperationException("no instances");
  }
}
