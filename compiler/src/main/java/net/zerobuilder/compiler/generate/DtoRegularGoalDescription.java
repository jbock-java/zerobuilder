package net.zerobuilder.compiler.generate;

import com.palantir.javapoet.CodeBlock;
import com.palantir.javapoet.TypeName;
import net.zerobuilder.compiler.generate.DtoRegularParameter.AbstractParameter;
import net.zerobuilder.compiler.generate.DtoRegularParameter.ProjectedParameter;
import net.zerobuilder.compiler.generate.DtoRegularParameter.SimpleParameter;

import java.util.List;

import static net.zerobuilder.compiler.generate.ZeroUtil.applyRanking;
import static net.zerobuilder.compiler.generate.ZeroUtil.createRanking;
import static net.zerobuilder.compiler.generate.ZeroUtil.joinCodeBlocks;

public final class DtoRegularGoalDescription {

  public sealed interface AbstractGoalDescription permits BuilderGoalDescription, UpdaterGoalDescription {
  }

  private static int[] createUnshuffle(
      List<? extends AbstractParameter> parameters,
      List<String> parameterNames) {
    String[] a = new String[parameters.size()];
    for (int i = 0; i < parameters.size(); i++) {
      a[i] = parameters.get(i).name();
    }
    String[] b = parameterNames.toArray(new String[0]);
    return createRanking(a, b);
  }

  public record BuilderGoalDescription(
      GoalDetails details,
      List<TypeName> thrownTypes,
      List<SimpleParameter> parameters,
      GoalContext context,
      int[] ranking) implements AbstractGoalDescription {

    public <E> List<E> unshuffle(List<E> shuffled) {
      return applyRanking(ranking, shuffled);
    }

    public CodeBlock invocationParameters() {
      List<SimpleParameter> unshuffled = unshuffle(parameters);
      return unshuffled.stream()
          .map(AbstractParameter::name)
          .map(CodeBlock::of)
          .collect(joinCodeBlocks(", "));
    }
  }

  static GoalContext getContext(AbstractGoalDescription description) {
    return switch (description) {
      case BuilderGoalDescription builder -> builder.context;
      case UpdaterGoalDescription updater -> updater.context;
    };
  }

  public record UpdaterGoalDescription(
      GoalDetails details,
      List<TypeName> thrownTypes,
      List<ProjectedParameter> parameters,
      GoalContext context) implements AbstractGoalDescription {
  }

  public static BuilderGoalDescription createBuilderGoalDescription(
      GoalDetails details,
      List<TypeName> thrownTypes,
      List<SimpleParameter> parameters,
      GoalContext context) {
    checkParameterNames(details.parameterNames(), parameters);
    int[] ranking = createUnshuffle(parameters, details.parameterNames());
    return new BuilderGoalDescription(details, thrownTypes, parameters, context, ranking);
  }

  public static UpdaterGoalDescription createUpdaterGoalDescription(
      GoalDetails details,
      List<TypeName> thrownTypes,
      List<ProjectedParameter> parameters,
      GoalContext context) {
    checkParameterNames(details.parameterNames(), parameters);
    return new UpdaterGoalDescription(details, thrownTypes, parameters, context);
  }

  private static void checkParameterNames(
      List<String> parameterNames,
      List<? extends AbstractParameter> parameters) {
    if (parameters.isEmpty()) {
      throw new IllegalArgumentException("need at least one parameter");
    }
    if (parameterNames.size() != parameters.size()) {
      throw new IllegalArgumentException("parameter names mismatch");
    }
    int[] positions = new int[parameterNames.size()];
    for (AbstractParameter parameter : parameters) {
      int i = parameterNames.indexOf(parameter.name());
      if (i < 0 || positions[i]++ != 0) {
        throw new IllegalArgumentException("parameter names mismatch: " + parameter.name());
      }
    }
  }

  private DtoRegularGoalDescription() {
  }
}
