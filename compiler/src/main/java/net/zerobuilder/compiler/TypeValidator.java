package net.zerobuilder.compiler;

import com.google.common.base.Predicate;
import com.google.common.collect.FluentIterable;
import com.google.common.collect.ImmutableList;
import com.squareup.javapoet.ClassName;
import net.zerobuilder.Build;
import net.zerobuilder.compiler.ValidationReport.ReportBuilder;

import javax.lang.model.element.ExecutableElement;
import javax.lang.model.element.NestingKind;
import javax.lang.model.element.TypeElement;
import java.util.EnumSet;

import static com.google.common.collect.Iterables.getOnlyElement;
import static javax.lang.model.element.ElementKind.CONSTRUCTOR;
import static javax.lang.model.element.Modifier.PRIVATE;
import static javax.lang.model.element.Modifier.STATIC;
import static javax.lang.model.element.NestingKind.MEMBER;
import static javax.lang.model.element.NestingKind.TOP_LEVEL;
import static javax.lang.model.util.ElementFilter.constructorsIn;
import static javax.lang.model.util.ElementFilter.methodsIn;
import static net.zerobuilder.compiler.Messages.ErrorMessages.GOAL_VIA_CONSTRUCTOR;
import static net.zerobuilder.compiler.Messages.ErrorMessages.METHOD_NOT_FOUND;
import static net.zerobuilder.compiler.Messages.ErrorMessages.NESTING_KIND;
import static net.zerobuilder.compiler.Messages.ErrorMessages.SEVERAL_METHODS;
import static net.zerobuilder.compiler.ValidationReport.about;

final class TypeValidator {

  private final EnumSet<NestingKind> allowedNestingKinds = EnumSet.of(TOP_LEVEL, MEMBER);

  ValidationReport<TypeElement, ExecutableElement> validateElement(TypeElement buildType, ClassName buildGoal) {
    ImmutableList<ExecutableElement> targetMethods = getTargetMethods(buildType);
    ReportBuilder<TypeElement, ExecutableElement> builder = about(buildType, ExecutableElement.class);
    if (targetMethods.isEmpty()) {
      return builder.error(METHOD_NOT_FOUND);
    }
    if (targetMethods.size() > 1) {
      return builder.error(SEVERAL_METHODS);
    }
    if (buildType.getModifiers().contains(PRIVATE)) {
      return builder.error(NESTING_KIND);
    }
    if (!allowedNestingKinds.contains(buildType.getNestingKind())
        || (buildType.getNestingKind() == MEMBER
        && !buildType.getModifiers().contains(STATIC))) {
      return builder.error(NESTING_KIND);
    }
    ExecutableElement buildVia = getOnlyElement(targetMethods);
    if (buildVia.getKind() == CONSTRUCTOR
        && !buildGoal.equals(ClassName.get(buildType))) {
      return builder.error(GOAL_VIA_CONSTRUCTOR);
    }
    return builder.clean(buildVia);
  }

  private static ImmutableList<ExecutableElement> getTargetMethods(TypeElement typeElement) {
    return FluentIterable.from(methodsIn(typeElement.getEnclosedElements()))
        .append(constructorsIn(typeElement.getEnclosedElements()))
        .filter(new Predicate<ExecutableElement>() {
          @Override
          public boolean apply(ExecutableElement element) {
            return element.getAnnotation(Build.Via.class) != null;
          }
        }).toList();
  }


}
