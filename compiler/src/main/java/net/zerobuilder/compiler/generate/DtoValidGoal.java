package net.zerobuilder.compiler.generate;

import com.google.common.base.Function;
import com.google.common.collect.ImmutableList;
import com.squareup.javapoet.TypeName;
import net.zerobuilder.compiler.generate.DtoBeanParameter.AbstractBeanParameter;
import net.zerobuilder.compiler.generate.DtoGoal.AbstractGoalDetails;
import net.zerobuilder.compiler.generate.DtoGoal.BeanGoalDetails;
import net.zerobuilder.compiler.generate.DtoGoal.RegularGoalDetails;
import net.zerobuilder.compiler.generate.DtoParameter.RegularParameter;

public final class DtoValidGoal {

  public interface ValidGoal {
    <R> R accept(ValidGoalCases<R> cases);
  }

  public interface ValidGoalCases<R> {
    R regularGoal(ValidRegularGoal goal);
    R beanGoal(ValidBeanGoal goal);
  }

  public static <R> Function<ValidGoal, R> asFunction(final ValidGoalCases<R> cases) {
    return new Function<ValidGoal, R>() {
      @Override
      public R apply(ValidGoal goal) {
        return goal.accept(cases);
      }
    };
  }

  public static final class ValidRegularGoal implements ValidGoal {
    public final RegularGoalDetails details;
    public final ImmutableList<TypeName> thrownTypes;
    public final ImmutableList<RegularParameter> parameters;

    public ValidRegularGoal(RegularGoalDetails details, ImmutableList<TypeName> thrownTypes, ImmutableList<RegularParameter> parameters) {
      this.details = details;
      this.thrownTypes = thrownTypes;
      this.parameters = parameters;
    }
    @Override
    public <R> R accept(ValidGoalCases<R> cases) {
      return cases.regularGoal(this);
    }
  }

  public static final class ValidBeanGoal implements ValidGoal {
    public final BeanGoalDetails details;
    public final ImmutableList<AbstractBeanParameter> parameters;

    public ValidBeanGoal(BeanGoalDetails details, ImmutableList<AbstractBeanParameter> parameters) {
      this.details = details;
      this.parameters = parameters;
    }
    @Override
    public <R> R accept(ValidGoalCases<R> cases) {
      return cases.beanGoal(this);
    }
  }

  static final Function<ValidGoal, AbstractGoalDetails> abstractGoal =
      asFunction(new ValidGoalCases<AbstractGoalDetails>() {
        @Override
        public AbstractGoalDetails regularGoal(ValidRegularGoal goal) {
          return goal.details;
        }
        @Override
        public AbstractGoalDetails beanGoal(ValidBeanGoal goal) {
          return goal.details;
        }
      });

  public static String goalName(ValidGoal goal) {
    return abstractGoal.apply(goal).name;
  }

  public static TypeName goalType(ValidGoal goal) {
    return DtoGoal.goalType.apply(DtoValidGoal.abstractGoal.apply(goal));
  }

  private DtoValidGoal() {
    throw new UnsupportedOperationException("no instances");
  }
}
