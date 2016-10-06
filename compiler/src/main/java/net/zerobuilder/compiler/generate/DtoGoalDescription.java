package net.zerobuilder.compiler.generate;

import com.google.common.base.Function;
import com.google.common.collect.ImmutableList;
import com.squareup.javapoet.TypeName;
import net.zerobuilder.compiler.generate.DtoBeanParameter.AbstractBeanParameter;
import net.zerobuilder.compiler.generate.DtoGoal.AbstractGoalDetails;
import net.zerobuilder.compiler.generate.DtoGoal.BeanGoalDetails;
import net.zerobuilder.compiler.generate.DtoGoal.RegularGoalDetails;
import net.zerobuilder.compiler.generate.DtoParameter.RegularParameter;

import java.util.List;

public final class DtoGoalDescription {

  public interface GoalDescription {
    <R> R accept(GoalDescriptionCases<R> cases);
  }

  interface GoalDescriptionCases<R> {
    R regularGoal(RegularGoalDescription goal);
    R beanGoal(BeanGoalDescription goal);
  }

  public static <R> Function<GoalDescription, R> asFunction(final GoalDescriptionCases<R> cases) {
    return new Function<GoalDescription, R>() {
      @Override
      public R apply(GoalDescription goal) {
        return goal.accept(cases);
      }
    };
  }

  /**
   * Describes of a goal that represents either a static method or an instance method, or a constructor.
   */
  public static final class RegularGoalDescription implements GoalDescription {
    final RegularGoalDetails details;
    final ImmutableList<TypeName> thrownTypes;
    final ImmutableList<RegularParameter> parameters;

    private RegularGoalDescription(RegularGoalDetails details,
                                   ImmutableList<TypeName> thrownTypes,
                                   ImmutableList<RegularParameter> parameters) {
      this.details = details;
      this.thrownTypes = thrownTypes;
      this.parameters = parameters;
    }

    public static RegularGoalDescription create(RegularGoalDetails details,
                                                List<TypeName> thrownTypes,
                                                List<RegularParameter> parameters) {
      return new RegularGoalDescription(details,
          ImmutableList.copyOf(thrownTypes),
          ImmutableList.copyOf(parameters));
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
    final ImmutableList<AbstractBeanParameter> parameters;

    private BeanGoalDescription(BeanGoalDetails details, ImmutableList<AbstractBeanParameter> parameters) {
      this.details = details;
      this.parameters = parameters;
    }

    public static BeanGoalDescription create(BeanGoalDetails details, List<AbstractBeanParameter> parameters) {
      return new BeanGoalDescription(details, ImmutableList.copyOf(parameters));
    }
    @Override
    public <R> R accept(GoalDescriptionCases<R> cases) {
      return cases.beanGoal(this);
    }
  }

  static final Function<GoalDescription, AbstractGoalDetails> abstractGoal =
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

  public static String goalName(GoalDescription goal) {
    return abstractGoal.apply(goal).name;
  }

  public static TypeName goalType(GoalDescription goal) {
    return DtoGoal.goalType.apply(DtoGoalDescription.abstractGoal.apply(goal));
  }

  private DtoGoalDescription() {
    throw new UnsupportedOperationException("no instances");
  }
}
