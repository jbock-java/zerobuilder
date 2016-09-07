package net.zerobuilder.compiler;

import com.google.common.base.Function;
import com.google.common.base.Optional;
import com.google.common.base.Predicate;
import com.google.common.collect.FluentIterable;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;
import com.squareup.javapoet.TypeName;
import net.zerobuilder.Step;

import javax.lang.model.element.ExecutableElement;
import javax.lang.model.element.TypeElement;
import javax.lang.model.element.VariableElement;
import javax.lang.model.util.Elements;

import static com.google.auto.common.MoreElements.getLocalAndInheritedMethods;
import static javax.lang.model.element.Modifier.PRIVATE;
import static javax.lang.model.element.Modifier.STATIC;
import static javax.lang.model.util.ElementFilter.fieldsIn;
import static javax.tools.Diagnostic.Kind.ERROR;
import static net.zerobuilder.compiler.Messages.ErrorMessages.DUPLICATE_STEP_POSITION;
import static net.zerobuilder.compiler.Messages.ErrorMessages.NEGATIVE_STEP_POSITION;
import static net.zerobuilder.compiler.Messages.ErrorMessages.STEP_POSITION_TOO_LARGE;
import static net.zerobuilder.compiler.Utilities.upcase;

final class ToBuilderValidator {

  private final ImmutableMap<String, ExecutableElement> methods;
  private final ImmutableMap<String, VariableElement> fields;
  private final ExecutableElement goal;

  private ToBuilderValidator(ImmutableMap<String, ExecutableElement> methodsByName,
                             ImmutableMap<String, VariableElement> fieldsByName,
                             ExecutableElement goal) {
    this.methods = methodsByName;
    this.fields = fieldsByName;
    this.goal = goal;
  }

  private static ToBuilderValidator createMatchValidator(TypeElement buildElement, ExecutableElement goal, Elements elements) {
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
    return new ToBuilderValidator(methodsByName, fieldsByName, goal);
  }

  ImmutableList<ValidParameter> validate() throws ValidationException {
    ImmutableList.Builder<TmpValidParameter> builder = ImmutableList.builder();
    for (VariableElement parameter : goal.getParameters()) {
      VariableElement field = fields.get(parameter.getSimpleName().toString());
      if (field != null && TypeName.get(field.asType()).equals(TypeName.get(parameter.asType()))) {
        builder.add(new TmpValidParameter(parameter, Optional.<String>absent()));
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
        builder.add(new TmpValidParameter(parameter, Optional.of(methodName)));
      }
    }
    return sortedParameters(builder.build());
  }

  ImmutableList<ValidParameter> skip() throws ValidationException {
    ImmutableList.Builder<TmpValidParameter> builder = ImmutableList.builder();
    for (VariableElement parameter : goal.getParameters()) {
      builder.add(new TmpValidParameter(parameter, Optional.<String>absent()));
    }
    return sortedParameters(builder.build());
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
    ToBuilderValidator buildElement(TypeElement buildElement) {
      return createMatchValidator(buildElement, targetMethod, elements);
    }
  }

  private static ImmutableList<ValidParameter> sortedParameters(ImmutableList<TmpValidParameter> parameters)
      throws ValidationException {
    ValidParameter[] builder = new ValidParameter[parameters.size()];
    ImmutableList.Builder<TmpValidParameter> noAnnotation = ImmutableList.builder();
    for (TmpValidParameter parameter : parameters) {
      Step step = parameter.parameter.getAnnotation(Step.class);
      if (step != null) {
        int value = step.value();
        if (value < 0) {
          throw new ValidationException(ERROR,
              NEGATIVE_STEP_POSITION, parameter.parameter);
        }
        if (value >= parameters.size()) {
          throw new ValidationException(ERROR,
              STEP_POSITION_TOO_LARGE, parameter.parameter);
        }
        if (builder[value] != null) {
          throw new ValidationException(ERROR,
              DUPLICATE_STEP_POSITION, parameter.parameter);
        }
        builder[value] = parameter.toValidParameter();
      } else {
        noAnnotation.add(parameter);
      }
    }
    int pos = 0;
    for (TmpValidParameter parameter : noAnnotation.build()) {
      while (builder[pos] != null) {
        pos++;
      }
      builder[pos++] = parameter.toValidParameter();
    }
    return ImmutableList.copyOf(builder);
  }

  static final class TmpValidParameter {
    final VariableElement parameter;
    final Optional<String> projectionMethodName;

    private TmpValidParameter(VariableElement parameter, Optional<String> projectionMethodName) {
      this.parameter = parameter;
      this.projectionMethodName = projectionMethodName;
    }
    ValidParameter toValidParameter() {
      return new ValidParameter(parameter.getSimpleName().toString(),
          TypeName.get(parameter.asType()), projectionMethodName);
    }
  }

  static final class ValidParameter {
    final String name;
    final TypeName type;
    final Optional<String> projectionMethodName;


    ValidParameter(String name, TypeName type, Optional<String> projectionMethodName) {
      this.name = name;
      this.type = type;
      this.projectionMethodName = projectionMethodName;
    }
  }

}
