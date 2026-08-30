package net.zerobuilder.compiler.generate;

import com.palantir.javapoet.ClassName;
import com.palantir.javapoet.CodeBlock;
import com.palantir.javapoet.TypeName;
import java.util.List;

import static net.zerobuilder.compiler.generate.ZeroUtil.applyRanking;

public record GoalDescription(
    GoalDetails details,
    List<TypeName> thrownTypes,
    List<ProjectedParameter> originalParameters,
    ClassName generatedType,
    int[] parameterRanking) {

  public CodeBlock invocationParameters() {
    return originalParameters.stream()
        .map(ProjectedParameter::name)
        .map(CodeBlock::of)
        .collect(CodeBlock.joining(", "));
  }

  public List<ProjectedParameter> parameters() {
    return applyRanking(parameterRanking, originalParameters);
  }
}
