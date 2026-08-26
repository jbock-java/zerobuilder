package net.zerobuilder.compiler.generate;

import com.palantir.javapoet.CodeBlock;
import com.palantir.javapoet.TypeName;
import java.util.List;

import static net.zerobuilder.compiler.generate.ZeroUtil.applyRanking;
import static net.zerobuilder.compiler.generate.ZeroUtil.joinCodeBlocks;

public record GoalDescription(
    GoalDetails details,
    List<TypeName> thrownTypes,
    List<ProjectedParameter> parameters,
    GoalContext context,
    int[] ranking) {

  public CodeBlock invocationParameters() {
    List<ProjectedParameter> unshuffled = applyRanking(ranking, parameters);
    return unshuffled.stream()
        .map(ProjectedParameter::name)
        .map(CodeBlock::of)
        .collect(joinCodeBlocks(", "));
  }
}
