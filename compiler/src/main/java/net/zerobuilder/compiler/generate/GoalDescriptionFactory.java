package net.zerobuilder.compiler.generate;

import com.palantir.javapoet.ClassName;
import com.palantir.javapoet.TypeName;
import java.util.List;

import static net.zerobuilder.compiler.generate.ZeroUtil.createRanking;

public final class GoalDescriptionFactory {

  private static int[] createShuffle(
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
    checkParameterNames(details.shuffledParameterNames(), parameters);
    int[] parameterRanking = createShuffle(parameters, details.shuffledParameterNames());
    return new GoalDescription(details, thrownTypes, parameters, generatedType, parameterRanking);
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
  }

  private GoalDescriptionFactory() {
  }
}
