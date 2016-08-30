package net.zerobuilder.compiler;

import com.google.common.collect.ImmutableList;
import net.zerobuilder.compiler.ValidationReport.ReportBuilder;

import javax.lang.model.element.ExecutableElement;
import javax.lang.model.element.NestingKind;
import javax.lang.model.element.TypeElement;
import java.util.EnumSet;

import static com.google.common.collect.Iterables.getOnlyElement;
import static javax.lang.model.element.Modifier.PRIVATE;
import static javax.lang.model.element.Modifier.STATIC;
import static javax.lang.model.element.NestingKind.MEMBER;
import static javax.lang.model.element.NestingKind.TOP_LEVEL;
import static net.zerobuilder.compiler.Messages.ErrorMessages.METHOD_NOT_FOUND;
import static net.zerobuilder.compiler.Messages.ErrorMessages.NESTING_KIND;
import static net.zerobuilder.compiler.Messages.ErrorMessages.SEVERAL_METHODS;
import static net.zerobuilder.compiler.ValidationReport.about;

final class TypeValidator {

  private final EnumSet<NestingKind> allowedNestingKinds = EnumSet.of(TOP_LEVEL, MEMBER);

  ValidationReport<TypeElement, ExecutableElement> validateElement(TypeElement element, ImmutableList<ExecutableElement> targetMethods) {
    ReportBuilder<TypeElement, ExecutableElement> builder = about(element, ExecutableElement.class);
    if (targetMethods.isEmpty()) {
      return builder.error(METHOD_NOT_FOUND);
    }
    if (targetMethods.size() > 1) {
      return builder.error(SEVERAL_METHODS);
    }
    if (element.getModifiers().contains(PRIVATE)) {
      return builder.error(NESTING_KIND);
    }
    if (!allowedNestingKinds.contains(element.getNestingKind())
        || (element.getNestingKind() == MEMBER
        && !element.getModifiers().contains(STATIC))) {
      return builder.error(NESTING_KIND);
    }
    return builder.clean(getOnlyElement(targetMethods));
  }

}
