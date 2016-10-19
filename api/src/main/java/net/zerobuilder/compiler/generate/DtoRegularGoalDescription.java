package net.zerobuilder.compiler.generate;

import com.squareup.javapoet.TypeName;
import net.zerobuilder.compiler.generate.DtoGoal.AbstractRegularGoalDetails;
import net.zerobuilder.compiler.generate.DtoGoalDescription.GoalDescription;
import net.zerobuilder.compiler.generate.DtoGoalDescription.GoalDescriptionCases;
import net.zerobuilder.compiler.generate.DtoRegularParameter.AbstractRegularParameter;
import net.zerobuilder.compiler.generate.DtoRegularParameter.ProjectedParameter;
import net.zerobuilder.compiler.generate.DtoRegularParameter.SimpleParameter;

import java.util.List;
import java.util.function.Function;

import static java.util.Collections.unmodifiableList;

public final class DtoRegularGoalDescription {

  public static abstract class RegularGoalDescription extends GoalDescription {
    final AbstractRegularGoalDetails details;
    final List<TypeName> thrownTypes;
    final List<AbstractRegularParameter> parameters() {
      return abstractParameters.apply(this);
    }

    protected RegularGoalDescription(AbstractRegularGoalDetails details, List<TypeName> thrownTypes) {
      this.details = details;
      this.thrownTypes = thrownTypes;
    }

    abstract <R> R acceptRegularGoalDescription(AbstractRegularGoalDescriptionCases<R> cases);
  }

  interface AbstractRegularGoalDescriptionCases<R> {
    R acceptSimple(SimpleRegularGoalDescription simple);
    R acceptProjected(ProjectedRegularGoalDescription projected);
  }

  static <R> Function<RegularGoalDescription, R> asFunction(AbstractRegularGoalDescriptionCases<R> cases) {
    return description -> description.acceptRegularGoalDescription(cases);
  }

  static <R> Function<RegularGoalDescription, R> regularGoalDescriptionCases(
      Function<SimpleRegularGoalDescription, R> simpleFunction,
      Function<ProjectedRegularGoalDescription, R> projectedFunction) {
    return asFunction(new AbstractRegularGoalDescriptionCases<R>() {
      @Override
      public R acceptSimple(SimpleRegularGoalDescription simple) {
        return simpleFunction.apply(simple);
      }
      @Override
      public R acceptProjected(ProjectedRegularGoalDescription projected) {
        return projectedFunction.apply(projected);
      }
    });
  }

  private static final Function<RegularGoalDescription, List<AbstractRegularParameter>> abstractParameters =
      regularGoalDescriptionCases(
          simple -> unmodifiableList(simple.parameters),
          projected -> unmodifiableList(projected.parameters));


  /**
   * Describes of a goal that represents either a static method or an instance method, or a constructor.
   */
  public static final class SimpleRegularGoalDescription extends RegularGoalDescription {

    final List<SimpleParameter> parameters;

    private SimpleRegularGoalDescription(AbstractRegularGoalDetails details,
                                         List<TypeName> thrownTypes,
                                         List<SimpleParameter> parameters) {
      super(details, thrownTypes);
      this.parameters = parameters;
    }

    public static SimpleRegularGoalDescription create(AbstractRegularGoalDetails details,
                                                      List<TypeName> thrownTypes,
                                                      List<SimpleParameter> parameters) {
      checkParameterNames(details.parameterNames, parameters);
      return new SimpleRegularGoalDescription(details, thrownTypes, parameters);
    }

    @Override
    public <R> R accept(GoalDescriptionCases<R> cases) {

      return cases.regularGoal(this);
    }
    @Override
    <R> R acceptRegularGoalDescription(AbstractRegularGoalDescriptionCases<R> cases) {
      return cases.acceptSimple(this);
    }
  }

  /**
   * Describes of a goal that represents either a static method or an instance method, or a constructor.
   */
  public static final class ProjectedRegularGoalDescription extends RegularGoalDescription {
    final List<ProjectedParameter> parameters;

    private ProjectedRegularGoalDescription(AbstractRegularGoalDetails details,
                                            List<TypeName> thrownTypes,
                                            List<ProjectedParameter> parameters) {
      super(details, thrownTypes);
      this.parameters = parameters;
    }

    public static ProjectedRegularGoalDescription create(AbstractRegularGoalDetails details,
                                                         List<TypeName> thrownTypes,
                                                         List<ProjectedParameter> parameters) {
      checkParameterNames(details.parameterNames, parameters);
      return new ProjectedRegularGoalDescription(details, thrownTypes, parameters);
    }

    @Override
    public <R> R accept(GoalDescriptionCases<R> cases) {
      return cases.regularGoal(this);
    }

    @Override
    <R> R acceptRegularGoalDescription(AbstractRegularGoalDescriptionCases<R> cases) {
      return cases.acceptProjected(this);
    }
  }

  private static void checkParameterNames(List<String> parameterNames,
                                          List<? extends AbstractRegularParameter> parameters) {
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


  private DtoRegularGoalDescription() {
    throw new UnsupportedOperationException("no instances");
  }
}
