package net.zerobuilder.compiler.generate;

import com.squareup.javapoet.CodeBlock;
import com.squareup.javapoet.TypeName;
import net.zerobuilder.compiler.generate.DtoGoalDetails.AbstractRegularDetails;
import net.zerobuilder.compiler.generate.DtoRegularParameter.AbstractRegularParameter;
import net.zerobuilder.compiler.generate.DtoRegularParameter.ProjectedParameter;
import net.zerobuilder.compiler.generate.DtoRegularParameter.SimpleParameter;

import java.util.List;

import static net.zerobuilder.compiler.generate.ZeroUtil.applyRanking;
import static net.zerobuilder.compiler.generate.ZeroUtil.createRanking;
import static net.zerobuilder.compiler.generate.ZeroUtil.joinCodeBlocks;

public final class DtoRegularGoalDescription {

  static int[] createUnshuffle(List<? extends AbstractRegularParameter> parameters, List<String> parameterNames) {
    String[] a = new String[parameters.size()];
    for (int i = 0; i < parameters.size(); i++) {
      a[i] = parameters.get(i).name;
    }
    String[] b = parameterNames.toArray(new String[parameterNames.size()]);
    return createRanking(a, b);
  }

  public static final class SimpleRegularGoalDescription {

    private final List<SimpleParameter> parameters;
    private final int[] ranking;
    private final AbstractRegularDetails details;
    private final List<TypeName> thrownTypes;

    public final List<TypeName> thrownTypes() {
      return thrownTypes;
    }

    public final AbstractRegularDetails details() {
      return details;
    }

    public final <E> List<E> unshuffle(List<E> shuffled) {
      return applyRanking(ranking, shuffled);
    }

    public final CodeBlock invocationParameters() {
      List<SimpleParameter> unshuffled = unshuffle(parameters);
      return unshuffled.stream()
          .map(parameter -> parameter.name)
          .map(CodeBlock::of)
          .collect(joinCodeBlocks(", "));
    }

    public List<SimpleParameter> parameters() {
      return parameters;
    }

    private SimpleRegularGoalDescription(AbstractRegularDetails details,
                                         List<TypeName> thrownTypes,
                                         List<SimpleParameter> parameters,
                                         int[] ranking) {
      this.details = details;
      this.thrownTypes = thrownTypes;
      this.ranking = ranking;
      this.parameters = parameters;
    }

    public static SimpleRegularGoalDescription create(AbstractRegularDetails details,
                                                      List<TypeName> thrownTypes,
                                                      List<SimpleParameter> parameters) {
      checkParameterNames(details.parameterNames, parameters);
      int[] ranking = createUnshuffle(parameters, details.parameterNames);
      return new SimpleRegularGoalDescription(details, thrownTypes, parameters, ranking);
    }
  }

  /**
   * Describes of a goal that represents either a static method or an instance method, or a constructor.
   */
  public static final class ProjectedRegularGoalDescription {
    private final List<ProjectedParameter> parameters;
    private final AbstractRegularDetails details;
    private final List<TypeName> thrownTypes;

    public List<ProjectedParameter> parameters() {
      return parameters;
    }

    public AbstractRegularDetails details() {
      return details;
    }

    public List<TypeName> thrownTypes() {
      return thrownTypes;
    }

    private ProjectedRegularGoalDescription(AbstractRegularDetails details,
                                            List<TypeName> thrownTypes,
                                            List<ProjectedParameter> parameters) {
      this.details = details;
      this.thrownTypes = thrownTypes;
      this.parameters = parameters;
    }

    public static ProjectedRegularGoalDescription create(AbstractRegularDetails details,
                                                         List<TypeName> thrownTypes,
                                                         List<ProjectedParameter> parameters) {
      checkParameterNames(details.parameterNames, parameters);
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
