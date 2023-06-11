package net.zerobuilder.compiler.generate;

import io.jbock.javapoet.CodeBlock;
import io.jbock.javapoet.TypeName;
import net.zerobuilder.compiler.generate.DtoContext.GoalContext;
import net.zerobuilder.compiler.generate.DtoGoalDetails.AbstractRegularDetails;
import net.zerobuilder.compiler.generate.DtoRegularParameter.AbstractRegularParameter;
import net.zerobuilder.compiler.generate.DtoRegularParameter.ProjectedParameter;
import net.zerobuilder.compiler.generate.DtoRegularParameter.SimpleParameter;

import java.util.List;

import static net.zerobuilder.compiler.generate.ZeroUtil.applyRanking;
import static net.zerobuilder.compiler.generate.ZeroUtil.createRanking;
import static net.zerobuilder.compiler.generate.ZeroUtil.joinCodeBlocks;

public final class DtoRegularGoalDescription {

  private static int[] createUnshuffle(List<? extends AbstractRegularParameter> parameters, List<String> parameterNames) {
    String[] a = new String[parameters.size()];
    for (int i = 0; i < parameters.size(); i++) {
      a[i] = parameters.get(i).name;
    }
    String[] b = parameterNames.toArray(new String[parameterNames.size()]);
    return createRanking(a, b);
  }

  public static final class SimpleRegularGoalDescription {

    private final int[] ranking;

    public final List<SimpleParameter> parameters;
    public final GoalContext context;
    public final AbstractRegularDetails details;
    public final List<TypeName> thrownTypes;

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

    private SimpleRegularGoalDescription(AbstractRegularDetails details,
                                         List<TypeName> thrownTypes,
                                         List<SimpleParameter> parameters,
                                         GoalContext context,
                                         int[] ranking) {
      this.details = details;
      this.thrownTypes = thrownTypes;
      this.ranking = ranking;
      this.parameters = parameters;
      this.context = context;
    }

    public static SimpleRegularGoalDescription create(AbstractRegularDetails details,
                                                      List<TypeName> thrownTypes,
                                                      List<SimpleParameter> parameters,
                                                      GoalContext context) {
      checkParameterNames(details.parameterNames, parameters);
      int[] ranking = createUnshuffle(parameters, details.parameterNames);
      return new SimpleRegularGoalDescription(details, thrownTypes, parameters, context, ranking);
    }
  }

  /**
   * Describes of a goal that represents either a static method or an instance method, or a constructor.
   */
  public static final class ProjectedRegularGoalDescription {
    public final List<ProjectedParameter> parameters;
    public final AbstractRegularDetails details;
    public final List<TypeName> thrownTypes;
    public final GoalContext context;

    private ProjectedRegularGoalDescription(AbstractRegularDetails details,
                                            List<TypeName> thrownTypes,
                                            List<ProjectedParameter> parameters, GoalContext context) {
      this.details = details;
      this.thrownTypes = thrownTypes;
      this.parameters = parameters;
      this.context = context;
    }

    public static ProjectedRegularGoalDescription create(AbstractRegularDetails details,
                                                         List<TypeName> thrownTypes,
                                                         List<ProjectedParameter> parameters,
                                                         GoalContext context) {
      checkParameterNames(details.parameterNames, parameters);
      return new ProjectedRegularGoalDescription(details, thrownTypes, parameters, context);
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
