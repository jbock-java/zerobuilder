package net.zerobuilder.compiler.generate;

import com.palantir.javapoet.CodeBlock;
import com.palantir.javapoet.TypeName;
import com.palantir.javapoet.TypeVariableName;
import java.util.List;
import javax.lang.model.element.Modifier;
import javax.lang.model.element.TypeElement;

/**
 * @param parameterNames parameter names in original order
 * @param access         goal options
 */
public record GoalDetails(
    TypeElement tel,
    List<String> parameterNames,
    Access access) {

  public List<TypeVariableName> instanceTypeParameters() {
    return tel.getTypeParameters().stream()
        .map(TypeVariableName::get)
        .toList();
  }

  public TypeName goalType() {
    return TypeName.get(tel.asType());
  }

  public Modifier[] getAccess(Modifier... modifiers) {
    return access == Access.PUBLIC ?
        ZeroUtil.addModifier(Modifier.PUBLIC, modifiers) :
        modifiers;
  }

  public CodeBlock invocationParameters() {
    return CodeBlock.of(String.join(", ", parameterNames));
  }
}
