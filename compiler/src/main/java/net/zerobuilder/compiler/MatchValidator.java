package net.zerobuilder.compiler;

import com.google.common.base.Function;
import com.google.common.base.Optional;
import com.google.common.collect.ImmutableListMultimap;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Multimaps;
import com.squareup.javapoet.TypeName;
import net.zerobuilder.compiler.MyContext.AccessType;

import javax.lang.model.element.ExecutableElement;
import javax.lang.model.element.TypeElement;
import javax.lang.model.element.VariableElement;
import javax.lang.model.util.Elements;
import java.util.List;

import static com.google.auto.common.MoreElements.getLocalAndInheritedMethods;
import static com.google.common.collect.Maps.uniqueIndex;
import static javax.lang.model.element.Modifier.PRIVATE;
import static javax.lang.model.element.Modifier.STATIC;
import static javax.lang.model.util.ElementFilter.fieldsIn;
import static net.zerobuilder.compiler.Messages.ErrorMessages.MATCH_ERROR;
import static net.zerobuilder.compiler.MyContext.AccessType.AUTOVALUE;
import static net.zerobuilder.compiler.MyContext.AccessType.FIELDS;
import static net.zerobuilder.compiler.MyContext.AccessType.GETTERS;

final class MatchValidator {

  private final ImmutableListMultimap<String, ExecutableElement> methodsByName;
  private final List<? extends VariableElement> parameters;
  private final TypeElement typeElement;

  private MatchValidator(ImmutableListMultimap<String, ExecutableElement> methodsByName, List<? extends VariableElement> parameters, TypeElement typeElement) {
    this.methodsByName = methodsByName;
    this.parameters = parameters;
    this.typeElement = typeElement;
  }

  static MatchValidator create(TypeElement typeElement, ExecutableElement targetMethod, Elements elements) {
    ImmutableSet<ExecutableElement> methods = getLocalAndInheritedMethods(typeElement, elements);
    ImmutableListMultimap<String, ExecutableElement> m = Multimaps.index(methods, new Function<ExecutableElement, String>() {
      @Override
      public String apply(ExecutableElement method) {
        return method.getSimpleName().toString();
      }
    });
    return new MatchValidator(m, targetMethod.getParameters(), typeElement);
  }

  ValidationReport<TypeElement, AccessType> validate() {
    ValidationReport.Builder<TypeElement, AccessType> builder = ValidationReport.about(typeElement, AccessType.class);
    Optional<AccessType> access = checkFieldAccess()
        .or(checkAutovalue())
        .or(checkGetters());
    if (!access.isPresent()) {
      return builder.error(MATCH_ERROR);
    }
    return builder.clean(access.get());
  }

  private Optional<AccessType> checkFieldAccess() {
    ImmutableMap<String, VariableElement> fieldsByName = uniqueIndex(fieldsIn(typeElement.getEnclosedElements()), new Function<VariableElement, String>() {
      @Override
      public String apply(VariableElement field) {
        return field.getSimpleName().toString();
      }
    });
    for (VariableElement parameter : parameters) {
      VariableElement field = fieldsByName.get(parameter.getSimpleName().toString());
      if (field == null
          || !TypeName.get(field.asType()).equals(TypeName.get(parameter.asType()))
          || field.getModifiers().contains(PRIVATE)
          || field.getModifiers().contains(STATIC)) {
        return Optional.absent();
      }
    }
    return Optional.of(FIELDS);
  }

  private Optional<AccessType> checkAutovalue() {
    // wip
    return Optional.of(AUTOVALUE);
  }

  private Optional<AccessType> checkGetters() {
    // wip
    return Optional.of(GETTERS);
  }

}
