package isobuilder.compiler;

import com.squareup.javapoet.ClassName;

import javax.lang.model.element.ExecutableElement;
import java.util.HashSet;
import java.util.Set;

import static isobuilder.compiler.ErrorMessages.DUPLICATE;

public final class DuplicateValidator {

  private final Set<ClassName> contractNames = new HashSet<>();

  ValidationReport<ExecutableElement> validateClassname(Target target) {
    ValidationReport.Builder<ExecutableElement> builder = ValidationReport.about(target.getExecutableElement());
    if (!contractNames.add(target.contractName())) {
      builder.addError(DUPLICATE);
    }
    return builder.build();
  }

}
