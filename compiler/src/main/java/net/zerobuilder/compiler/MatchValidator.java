package net.zerobuilder.compiler;

import com.google.common.base.Function;
import com.google.common.base.Optional;
import com.google.common.base.Predicate;
import com.google.common.collect.FluentIterable;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;
import com.squareup.javapoet.TypeName;
import net.zerobuilder.compiler.MyContext.AccessType;
import net.zerobuilder.compiler.ValidationReport.ReportBuilder;

import javax.lang.model.element.ExecutableElement;
import javax.lang.model.element.Modifier;
import javax.lang.model.element.TypeElement;
import javax.lang.model.element.VariableElement;
import javax.lang.model.util.Elements;
import java.util.List;

import static com.google.auto.common.MoreElements.getLocalAndInheritedMethods;
import static com.google.common.collect.Maps.uniqueIndex;
import static com.google.common.collect.Sets.immutableEnumSet;
import static com.google.common.collect.Sets.intersection;
import static javax.lang.model.element.Modifier.PRIVATE;
import static javax.lang.model.element.Modifier.STATIC;
import static javax.lang.model.util.ElementFilter.fieldsIn;
import static net.zerobuilder.compiler.Messages.ErrorMessages.MATCH_ERROR;
import static net.zerobuilder.compiler.MyContext.AccessType.AUTOVALUE;
import static net.zerobuilder.compiler.MyContext.AccessType.FIELDS;
import static net.zerobuilder.compiler.MyContext.AccessType.GETTERS;
import static net.zerobuilder.compiler.MyContext.AccessType.NONE;
import static net.zerobuilder.compiler.Util.upcase;
import static net.zerobuilder.compiler.ValidationReport.about;

final class MatchValidator {

  private final ImmutableMap<String, ExecutableElement> methodsByName;
  private final List<? extends VariableElement> parameters;
  private final TypeElement typeElement;

  private static final ImmutableSet<Modifier> BAD_MODIFIERS = immutableEnumSet(PRIVATE, STATIC);

  private MatchValidator(ImmutableMap<String, ExecutableElement> methodsByName, List<? extends VariableElement> parameters, TypeElement typeElement) {
    this.methodsByName = methodsByName;
    this.parameters = parameters;
    this.typeElement = typeElement;
  }

  private static MatchValidator createMatchValidator(TypeElement buildElement, ExecutableElement buildVia, Elements elements) {
    ImmutableSet<ExecutableElement> methods = getLocalAndInheritedMethods(buildElement, elements);
    ImmutableMap<String, ExecutableElement> map = FluentIterable.from(methods)
        .filter(new Predicate<ExecutableElement>() {
          @Override
          public boolean apply(ExecutableElement method) {
            return method.getParameters().isEmpty();
          }
        })
        .uniqueIndex(new Function<ExecutableElement, String>() {
          @Override
          public String apply(ExecutableElement method) {
            return method.getSimpleName().toString();
          }
        });
    return new MatchValidator(map, buildVia.getParameters(), buildElement);
  }

  ValidationReport<TypeElement, AccessType> validate() {
    ReportBuilder<TypeElement, AccessType> builder = about(typeElement, AccessType.class);
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
          || !intersection(BAD_MODIFIERS, field.getModifiers()).isEmpty()) {
        return Optional.absent();
      }
    }
    return Optional.of(FIELDS);
  }

  private Optional<AccessType> checkAutovalue() {
    for (VariableElement parameter : parameters) {
      ExecutableElement method = methodsByName.get(parameter.getSimpleName().toString());
      if (method == null
          || !TypeName.get(method.getReturnType()).equals(TypeName.get(parameter.asType()))
          || !intersection(BAD_MODIFIERS, method.getModifiers()).isEmpty())
        return Optional.absent();
    }
    return Optional.of(AUTOVALUE);
  }

  private Optional<AccessType> checkGetters() {
    for (VariableElement parameter : parameters) {
      ExecutableElement method = methodsByName.get("get" + upcase(parameter.getSimpleName().toString()));
      if (method == null
          || !TypeName.get(method.getReturnType()).equals(TypeName.get(parameter.asType()))
          || !intersection(BAD_MODIFIERS, method.getModifiers()).isEmpty())
        return Optional.absent();
    }
    return Optional.of(GETTERS);
  }

  static class BuilderFactory {
    private final Elements elements;

    BuilderFactory(Elements elements) {
      this.elements = elements;
    }
    Builder buildViaElement(ExecutableElement buildVia) {
      return new Builder().elements(elements).buildViaElement(buildVia);
    }
  }


  static ValidationReport<TypeElement, AccessType> skipMatchValidation(TypeElement typeElement) {
    ReportBuilder<TypeElement, AccessType> builder = about(typeElement, AccessType.class);
    return builder.clean(NONE);
  }

  static class Builder {
    private ExecutableElement targetMethod;
    private Elements elements;
    private Builder buildViaElement(ExecutableElement targetMethod) {
      this.targetMethod = targetMethod;
      return this;
    }
    private Builder elements(Elements elements) {
      this.elements = elements;
      return this;
    }
    MatchValidator buildElement(TypeElement buildElement) {
      return createMatchValidator(buildElement, targetMethod, elements);
    }
  }

}
