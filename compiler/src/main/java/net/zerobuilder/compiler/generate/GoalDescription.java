package net.zerobuilder.compiler.generate;

import com.palantir.javapoet.ClassName;
import com.palantir.javapoet.CodeBlock;
import java.util.List;

import static net.zerobuilder.compiler.generate.ZeroUtil.applyRanking;

public record GoalDescription(
    GoalDetails details,
    List<ProjectedParameter> originalParameters,
    ClassName generatedType,
    int[] parameterRanking) {

  public CodeBlock invocationParameters() {
    return originalParameters.stream()
        .map(ProjectedParameter::stepName)
        .map(CodeBlock::of)
        .collect(CodeBlock.joining(", "));
  }

  public List<ProjectedParameter> parameters() {
    return applyRanking(parameterRanking, originalParameters);
  }

  public boolean classicMode() {
    return details.annotation().classicMode();
  }

  public boolean createOnly() {
    return details.annotation().createOnly();
  }
}
