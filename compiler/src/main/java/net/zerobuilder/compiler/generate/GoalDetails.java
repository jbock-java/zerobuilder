package net.zerobuilder.compiler.generate;

import com.palantir.javapoet.ClassName;
import com.palantir.javapoet.CodeBlock;
import com.palantir.javapoet.TypeName;
import com.palantir.javapoet.TypeVariableName;
import java.util.List;
import javax.lang.model.element.Modifier;

import static net.zerobuilder.compiler.generate.ZeroUtil.parameterizedTypeName;

/**
 * @param name           goal name
 * @param parameterNames parameter names in original order
 * @param access         goal options
 */
public record GoalDetails(
    TypeName goalType,
    String name,
    List<String> parameterNames,
    Access access,
    List<TypeVariableName> instanceTypeParameters) {

  public static GoalDetails createGoalDetails(
      ClassName goalType,
      String name,
      List<String> parameterNames,
      Access access,
      List<TypeVariableName> instanceTypeParameters) {
    return new GoalDetails(
        parameterizedTypeName(goalType, instanceTypeParameters),
        name,
        parameterNames,
        access,
        instanceTypeParameters);
  }

  public Modifier[] access(Modifier modifiers) {
    return ZeroUtil.modifiers(access, modifiers);
  }

  public CodeBlock invocationParameters() {
    return CodeBlock.of(String.join(", ", parameterNames));
  }
}
