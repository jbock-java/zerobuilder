package net.zerobuilder.compiler.generate;

import com.squareup.javapoet.TypeName;
import net.zerobuilder.compiler.generate.DtoBeanParameter.AbstractBeanParameter;
import net.zerobuilder.compiler.generate.DtoGoal.AbstractGoalDetails;
import net.zerobuilder.compiler.generate.DtoGoal.BeanGoalDetails;
import net.zerobuilder.compiler.generate.DtoGoal.RegularGoalDetails;
import net.zerobuilder.compiler.generate.DtoRegularParameter.AbstractRegularParameter;

import java.util.List;
import java.util.Optional;
import java.util.function.Function;

public final class DtoGoalDescription {

  public interface GoalDescription {
    <R> R accept(GoalDescriptionCases<R> cases);
  }

  interface GoalDescriptionCases<R> {
    R regularGoal(RegularGoalDescription goal);
    R beanGoal(BeanGoalDescription goal);
  }

  static <R> Function<GoalDescription, R> asFunction(final GoalDescriptionCases<R> cases) {
    return goal -> goal.accept(cases);
  }

  static <R> Function<GoalDescription, R> goalDescriptionCases(
      Function<? super RegularGoalDescription, ? extends R> regular,
      Function<? super BeanGoalDescription, ? extends R> bean) {
    return asFunction(new GoalDescriptionCases<R>() {
      @Override
      public R regularGoal(RegularGoalDescription goal) {
        return regular.apply(goal);
      }
      @Override
      public R beanGoal(BeanGoalDescription goal) {
        return bean.apply(goal);
      }
    });
  }

  static final Function<GoalDescription, AbstractGoalDetails> goalDetails =
      goalDescriptionCases(
          description -> description.details,
          description -> description.details);

  /**
   * Describes of a goal that represents either a static method or an instance method, or a constructor.
   */
  public static final class RegularGoalDescription implements GoalDescription {
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
      validateProjectionInfo(details.goalOptions, parameters);
      return new RegularGoalDescription(details, thrownTypes, parameters);
    }

    private static void validateProjectionInfo(DtoGoal.GoalOptions options,
                                               List<AbstractRegularParameter> parameters) {
      if (!options.needsProjections()) {
        return;
      }
      if (parameters.stream()
          .map(parameter -> parameter.projectionInfo())
          .allMatch(Optional::isPresent)) {
        return;
      }
      throw new IllegalStateException("projection info required");
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

  /**
   * Describes the task of creating and / or updating a JavaBean.
   */
  public static final class BeanGoalDescription implements GoalDescription {
    final BeanGoalDetails details;
    final List<AbstractBeanParameter> parameters;
    final List<TypeName> thrownTypes;

    private BeanGoalDescription(BeanGoalDetails details, List<AbstractBeanParameter> parameters,
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

  private static final Function<GoalDescription, AbstractGoalDetails> abstractGoal =
      asFunction(new GoalDescriptionCases<AbstractGoalDetails>() {
        @Override
        public AbstractGoalDetails regularGoal(RegularGoalDescription goal) {
          return goal.details;
        }
        @Override
        public AbstractGoalDetails beanGoal(BeanGoalDescription goal) {
          return goal.details;
        }
      });

  static String goalName(GoalDescription goal) {
    return abstractGoal.apply(goal).name;
  }

  static TypeName goalType(GoalDescription goal) {
    return DtoGoal.goalType.apply(DtoGoalDescription.abstractGoal.apply(goal));
  }

  private DtoGoalDescription() {
    throw new UnsupportedOperationException("no instances");
  }
}
