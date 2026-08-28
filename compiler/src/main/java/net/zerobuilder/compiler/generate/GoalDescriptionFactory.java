package net.zerobuilder.compiler.generate;

import com.palantir.javapoet.ClassName;
import com.palantir.javapoet.TypeName;

import java.util.List;

import static net.zerobuilder.compiler.generate.ZeroUtil.createRanking;

public final class GoalDescriptionFactory {

  private static int[] createUnshuffle(
      List<ProjectedParameter> parameters,
      List<String> parameterNames) {
    String[] a = new String[parameters.size()];
    for (int i = 0; i < parameters.size(); i++) {
      a[i] = parameters.get(i).name();
    }
    String[] b = parameterNames.toArray(new String[0]);
    return createRanking(a, b);
  }

  public static GoalDescription createTheGoalDescription(
      GoalDetails details,
      List<TypeName> thrownTypes,
      List<ProjectedParameter> parameters,
      ClassName generatedType) {
    checkParameterNames(details.parameterNames(), parameters);
    int[] ranking = createUnshuffle(parameters, details.parameterNames());
    return new GoalDescription(details, thrownTypes, parameters, generatedType, ranking);
  }

  private static void checkParameterNames(
      List<String> parameterNames,
      List<ProjectedParameter> parameters) {
    if (parameters.isEmpty()) {
      throw new IllegalArgumentException("need at least one parameter");
    }
    if (parameterNames.size() != parameters.size()) {
      throw new IllegalArgumentException("parameter names mismatch");
    }
    int[] positions = new int[parameterNames.size()];
    for (ProjectedParameter parameter : parameters) {
      int i = parameterNames.indexOf(parameter.name());
      if (i < 0 || positions[i]++ != 0) {
        throw new IllegalArgumentException("parameter names mismatch: " + parameter.name());
      }
    }
  }

  private GoalDescriptionFactory() {
  }
}
