package net.zerobuilder.compiler;

import com.google.common.base.Function;
import com.google.common.base.Optional;
import com.google.common.base.Predicate;
import com.google.common.collect.FluentIterable;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;
import com.squareup.javapoet.TypeName;

import javax.lang.model.element.ExecutableElement;
import javax.lang.model.element.TypeElement;
import javax.lang.model.element.VariableElement;
import javax.lang.model.util.Elements;

import static com.google.auto.common.MoreElements.getLocalAndInheritedMethods;
import static javax.lang.model.element.Modifier.PRIVATE;
import static javax.lang.model.element.Modifier.STATIC;
import static javax.lang.model.util.ElementFilter.fieldsIn;
import static net.zerobuilder.compiler.Util.upcase;

final class MatchValidator {

  private final ImmutableMap<String, ExecutableElement> methods;
  private final ImmutableMap<String, VariableElement> fields;
  private final ExecutableElement goal;

  private MatchValidator(ImmutableMap<String, ExecutableElement> methodsByName,
                         ImmutableMap<String, VariableElement> fieldsByName,
                         ExecutableElement goal) {
    this.methods = methodsByName;
    this.fields = fieldsByName;
    this.goal = goal;
  }

  private static MatchValidator createMatchValidator(TypeElement buildElement, ExecutableElement goal, Elements elements) {
    ImmutableSet<ExecutableElement> methods = getLocalAndInheritedMethods(buildElement, elements);
    ImmutableMap<String, ExecutableElement> methodsByName = FluentIterable.from(methods)
        .filter(new Predicate<ExecutableElement>() {
          @Override
          public boolean apply(ExecutableElement method) {
            return method.getParameters().isEmpty()
                && !method.getModifiers().contains(PRIVATE) && !method.getModifiers().contains(STATIC);
          }
        })
        .uniqueIndex(new Function<ExecutableElement, String>() {
          @Override
          public String apply(ExecutableElement method) {
            return method.getSimpleName().toString();
          }
        });
    ImmutableMap<String, VariableElement> fieldsByName
        = FluentIterable.from(fieldsIn(buildElement.getEnclosedElements()))
        .filter(new Predicate<VariableElement>() {
          @Override
          public boolean apply(VariableElement field) {
            return !field.getModifiers().contains(PRIVATE) && !field.getModifiers().contains(STATIC);
          }
        })
        .uniqueIndex(new Function<VariableElement, String>() {
          @Override
          public String apply(VariableElement field) {
            return field.getSimpleName().toString();
          }
        });
    return new MatchValidator(methodsByName, fieldsByName, goal);
  }

  ImmutableList<ProjectionInfo> validate() throws ValidationException {
    ImmutableList.Builder<ProjectionInfo> builder = ImmutableList.builder();
    for (VariableElement parameter : goal.getParameters()) {
      VariableElement field = fields.get(parameter.getSimpleName().toString());
      if (field != null && TypeName.get(field.asType()).equals(TypeName.get(parameter.asType()))) {
        builder.add(new ProjectionInfo(parameter, Optional.<String>absent()));
      } else {
        String methodName = "get" + upcase(parameter.getSimpleName().toString());
        ExecutableElement method = methods.get(methodName);
        if (method == null
            && TypeName.get(parameter.asType()) == TypeName.BOOLEAN) {
          methodName = "is" + upcase(parameter.getSimpleName().toString());
          method = methods.get(methodName);
        }
        if (method == null) {
          methodName = parameter.getSimpleName().toString();
          method = methods.get(methodName);
        }
        if (method == null) {
          throw new ValidationException("Could not find projection for parameter: "
              + parameter.getSimpleName(), goal);
        }
        builder.add(new ProjectionInfo(parameter, Optional.of(methodName)));
      }
    }
    return builder.build();
  }

  ImmutableList<ProjectionInfo> skip() {
    ImmutableList.Builder<ProjectionInfo> builder = ImmutableList.builder();
    for (VariableElement parameter : goal.getParameters()) {
      builder.add(new ProjectionInfo(parameter, Optional.<String>absent()));
    }
    return builder.build();
  }

  static final class Factory {
    private final Elements elements;

    Factory(Elements elements) {
      this.elements = elements;
    }
    Builder buildViaElement(ExecutableElement buildVia) {
      return new Builder().elements(elements).buildViaElement(buildVia);
    }
  }

  static final class Builder {
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

  static final class ProjectionInfo {
    final VariableElement parameter;
    final Optional<String> projectionMethodName;

    ProjectionInfo(VariableElement parameter, Optional<String> projectionMethodName) {
      this.parameter = parameter;
      this.projectionMethodName = projectionMethodName;
    }

  }

}
