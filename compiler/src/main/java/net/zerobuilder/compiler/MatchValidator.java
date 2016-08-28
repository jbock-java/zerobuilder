package net.zerobuilder.compiler;

import com.google.common.collect.ImmutableSet;
import net.zerobuilder.compiler.Target.AccessType;

import javax.lang.model.element.ExecutableElement;
import javax.lang.model.element.TypeElement;
import javax.lang.model.util.Elements;

import static com.google.auto.common.MoreElements.getLocalAndInheritedMethods;
import static net.zerobuilder.compiler.Messages.ErrorMessages.MATCH_ERROR;
import static net.zerobuilder.compiler.Target.AccessType.AUTOVALUE;

final class MatchValidator {

  private final Elements elements;

  MatchValidator(Elements elements) {
    this.elements = elements;
  }

  ValidationReport<TypeElement, AccessType> validateElement(TypeElement typeElement, ExecutableElement targetMethod) {
    ValidationReport.Builder<TypeElement, AccessType> builder = ValidationReport.about(typeElement, AccessType.class);
    ImmutableSet<ExecutableElement> methods = getLocalAndInheritedMethods(typeElement, elements);
    if (methods.size() < targetMethod.getParameters().size() + 1) {
      return builder.error(MATCH_ERROR);
    }
    // hardcoded for now
    return builder.clean(AUTOVALUE);
  }

}
