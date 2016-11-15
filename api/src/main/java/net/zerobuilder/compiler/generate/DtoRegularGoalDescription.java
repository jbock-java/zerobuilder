package net.zerobuilder.compiler.generate;

import com.squareup.javapoet.TypeName;
import net.zerobuilder.compiler.generate.DtoGoalDetails.AbstractRegularDetails;
import net.zerobuilder.compiler.generate.DtoGoalDetails.ProjectableDetails;
import net.zerobuilder.compiler.generate.DtoGoalDetails.StaticMethodGoalDetails;
import net.zerobuilder.compiler.generate.DtoProjectedDescription.ProjectedDescription;
import net.zerobuilder.compiler.generate.DtoProjectedDescription.ProjectedDescriptionCases;
import net.zerobuilder.compiler.generate.DtoRegularParameter.AbstractRegularParameter;
import net.zerobuilder.compiler.generate.DtoRegularParameter.ProjectedParameter;
import net.zerobuilder.compiler.generate.DtoRegularParameter.SimpleParameter;
import net.zerobuilder.compiler.generate.DtoSimpleDescription.SimpleDescription;
import net.zerobuilder.compiler.generate.DtoSimpleDescription.SimpleDescriptionCases;

import java.util.List;
import java.util.function.Function;

import static java.util.Collections.unmodifiableList;
import static net.zerobuilder.compiler.generate.DtoGoalDetails.parameterNames;

public final class DtoRegularGoalDescription {

  interface AbstractRegularGoalDescriptionCases<R> {
    R acceptSimple(SimpleRegularGoalDescription simple);
    R acceptProjected(ProjectedRegularGoalDescription projected);
  }

  static <R> Function<AbstractRegularGoalDescription, R> asFunction(AbstractRegularGoalDescriptionCases<R> cases) {
    return description -> description.acceptRegularGoalDescription(cases);
  }

  static <R> Function<AbstractRegularGoalDescription, R> regularGoalDescriptionCases(
      Function<SimpleRegularGoalDescription, ? extends R> simpleFunction,
      Function<ProjectedRegularGoalDescription, ? extends R> projectedFunction) {
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

  public static abstract class AbstractRegularGoalDescription {
    final AbstractRegularDetails details;
    final List<TypeName> thrownTypes;
    final List<AbstractRegularParameter> parameters() {
      return abstractParameters.apply(this);
    }

    protected AbstractRegularGoalDescription(AbstractRegularDetails details, List<TypeName> thrownTypes) {
      this.details = details;
      this.thrownTypes = thrownTypes;
    }

    abstract <R> R acceptRegularGoalDescription(AbstractRegularGoalDescriptionCases<R> cases);
  }


  private static final Function<AbstractRegularGoalDescription, List<AbstractRegularParameter>> abstractParameters =
      regularGoalDescriptionCases(
          simple -> unmodifiableList(simple.parameters),
          projected -> unmodifiableList(projected.parameters));


  /**
   * Describes of a goal that represents either a static method or an instance method, or a constructor.
   */
  public static final class SimpleRegularGoalDescription extends AbstractRegularGoalDescription
      implements SimpleDescription {

    final List<SimpleParameter> parameters;

    private SimpleRegularGoalDescription(AbstractRegularDetails details,
                                         List<TypeName> thrownTypes,
                                         List<SimpleParameter> parameters) {
      super(details, thrownTypes);
      this.parameters = parameters;
    }

    public static SimpleRegularGoalDescription create(AbstractRegularDetails details,
                                                      List<TypeName> thrownTypes,
                                                      List<SimpleParameter> parameters) {
      checkParameterNames(details.parameterNames, parameters);
      return new SimpleRegularGoalDescription(details, thrownTypes, parameters);
    }

    @Override
    <R> R acceptRegularGoalDescription(AbstractRegularGoalDescriptionCases<R> cases) {
      return cases.acceptSimple(this);
    }

    @Override
    public <R> R acceptSimple(SimpleDescriptionCases<R> cases) {
      return cases.regular(this);
    }
  }

  /**
   * <em>The name is misleading</em>
   * Describes of a goal that represents either a static method or an instance method, or a constructor.
   */
  public static final class SimpleStaticGoalDescription {

    final List<SimpleParameter> parameters;
    final StaticMethodGoalDetails details;
    final List<TypeName> thrownTypes;

    private SimpleStaticGoalDescription(StaticMethodGoalDetails details,
                                        List<TypeName> thrownTypes,
                                        List<SimpleParameter> parameters) {
      this.details = details;
      this.parameters = parameters;
      this.thrownTypes = thrownTypes;
    }

    public static SimpleStaticGoalDescription create(StaticMethodGoalDetails details,
                                                     List<TypeName> thrownTypes,
                                                     List<SimpleParameter> parameters) {
      checkParameterNames(details.parameterNames, parameters);
      return new SimpleStaticGoalDescription(details, thrownTypes, parameters);
    }
  }

  /**
   * Describes of a goal that represents either a static method or an instance method, or a constructor.
   */
  public static final class ProjectedRegularGoalDescription
      implements ProjectedDescription {
    final List<ProjectedParameter> parameters;
    final ProjectableDetails details;
    final List<TypeName> thrownTypes;


    private ProjectedRegularGoalDescription(ProjectableDetails details,
                                            List<TypeName> thrownTypes,
                                            List<ProjectedParameter> parameters) {
      this.details = details;
      this.thrownTypes = thrownTypes;
      this.parameters = parameters;
    }

    public static ProjectedRegularGoalDescription create(ProjectableDetails details,
                                                         List<TypeName> thrownTypes,
                                                         List<ProjectedParameter> parameters) {
      checkParameterNames(parameterNames.apply(details), parameters);
      return new ProjectedRegularGoalDescription(details, thrownTypes, parameters);
    }

    @Override
    public <R> R acceptProjected(ProjectedDescriptionCases<R> cases) {
      return cases.regular(this);
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
