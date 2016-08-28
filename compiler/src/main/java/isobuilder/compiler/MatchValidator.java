package isobuilder.compiler;

import com.google.common.collect.ImmutableSet;

import javax.lang.model.element.ExecutableElement;
import javax.lang.model.element.TypeElement;
import javax.lang.model.util.Elements;

import static com.google.auto.common.MoreElements.getLocalAndInheritedMethods;
import static isobuilder.compiler.ErrorMessages.MATCH_ERROR;

final class MatchValidator {

  private final Elements elements;

  MatchValidator(Elements elements) {
    this.elements = elements;
  }

  ValidationReport validateElement(TypeElement typeElement, ExecutableElement targetMethod) {
    ValidationReport.Builder<TypeElement> builder = ValidationReport.about(typeElement);
    ImmutableSet<ExecutableElement> methods = getLocalAndInheritedMethods(typeElement, elements);
    if (methods.size() < targetMethod.getParameters().size() + 1) {
      return builder.error(MATCH_ERROR);
    }
    // more checks
    return builder.clean();
  }

}
