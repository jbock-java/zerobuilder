package net.zerobuilder.compiler.generate;

import com.squareup.javapoet.TypeName;
import net.zerobuilder.compiler.generate.DtoGoalDetails.AbstractRegularDetails;
import net.zerobuilder.compiler.generate.DtoGoalDetails.ProjectableDetails;
import net.zerobuilder.compiler.generate.DtoRegularParameter.AbstractRegularParameter;
import net.zerobuilder.compiler.generate.DtoRegularParameter.ProjectedParameter;
import net.zerobuilder.compiler.generate.DtoRegularParameter.SimpleParameter;

import java.util.List;

import static net.zerobuilder.compiler.generate.DtoGoalDetails.parameterNames;

public final class DtoRegularGoalDescription {

  public static abstract class AbstractRegularGoalDescription {
    private final AbstractRegularDetails details;
    private final List<TypeName> thrownTypes;

    public final List<TypeName> thrownTypes() {
      return thrownTypes;
    }

    public final AbstractRegularDetails details() {
      return details;
    }

    protected AbstractRegularGoalDescription(AbstractRegularDetails details, List<TypeName> thrownTypes) {
      this.details = details;
      this.thrownTypes = thrownTypes;
    }
  }

  /**
   * Describes of a goal that represents either a static method or an instance method, or a constructor.
   */
  public static final class SimpleRegularGoalDescription extends AbstractRegularGoalDescription {

    private final List<SimpleParameter> parameters;

    public List<SimpleParameter> parameters() {
      return parameters;
    }

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
  }

  /**
   * Describes of a goal that represents either a static method or an instance method, or a constructor.
   */
  public static final class ProjectedRegularGoalDescription {
    private final List<ProjectedParameter> parameters;
    private final ProjectableDetails details;
    private final List<TypeName> thrownTypes;

    public List<ProjectedParameter> parameters() {
      return parameters;
    }

    public ProjectableDetails details() {
      return details;
    }

    public List<TypeName> thrownTypes() {
      return thrownTypes;
    }

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
