package net.zerobuilder.compiler.generate;

import com.palantir.javapoet.ClassName;
import com.palantir.javapoet.CodeBlock;
import com.palantir.javapoet.TypeName;
import java.util.List;

import static net.zerobuilder.compiler.generate.ZeroUtil.applyRanking;

public record GoalDescription(
    GoalDetails details,
    List<TypeName> thrownTypes,
    List<ProjectedParameter> parameters,
    ClassName generatedType,
    int[] parameterRanking) {

  public CodeBlock invocationParameters() {
    List<ProjectedParameter> unshuffled = applyRanking(parameterRanking, parameters);
    return unshuffled.stream()
        .map(ProjectedParameter::name)
        .map(CodeBlock::of)
        .collect(CodeBlock.joining(", "));
  }
}
