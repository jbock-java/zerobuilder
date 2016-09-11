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
import net.zerobuilder.compiler.Analyser.GoalElement;
import net.zerobuilder.compiler.GoalContextFactory.GoalKind;

import javax.lang.model.element.Element;
import javax.lang.model.element.ElementKind;
import javax.lang.model.element.ExecutableElement;
import javax.lang.model.element.TypeElement;
import javax.lang.model.element.VariableElement;
import javax.lang.model.type.TypeKind;
import javax.lang.model.util.ElementFilter;
import javax.lang.model.util.Elements;
import java.util.List;

import static com.google.auto.common.MoreElements.getLocalAndInheritedMethods;
import static com.google.common.base.Ascii.isUpperCase;
import static javax.lang.model.element.Modifier.PRIVATE;
import static javax.lang.model.element.Modifier.PUBLIC;
import static javax.lang.model.element.Modifier.STATIC;
import static javax.lang.model.element.NestingKind.MEMBER;
import static javax.lang.model.util.ElementFilter.fieldsIn;
import static javax.tools.Diagnostic.Kind.ERROR;
import static net.zerobuilder.compiler.Messages.ErrorMessages.DUPLICATE_STEP_POSITION;
import static net.zerobuilder.compiler.Messages.ErrorMessages.NEGATIVE_STEP_POSITION;
import static net.zerobuilder.compiler.Messages.ErrorMessages.NO_DEFAULT_CONSTRUCTOR;
import static net.zerobuilder.compiler.Messages.ErrorMessages.NO_SETTERS;
import static net.zerobuilder.compiler.Messages.ErrorMessages.STEP_POSITION_TOO_LARGE;
import static net.zerobuilder.compiler.Messages.ErrorMessages.TARGET_NESTING_KIND;
import static net.zerobuilder.compiler.Messages.ErrorMessages.TARGET_PUBLIC;
import static net.zerobuilder.compiler.TypeValidator.ALLOWED_NESTING_KINDS;
import static net.zerobuilder.compiler.Utilities.downcase;
import static net.zerobuilder.compiler.Utilities.upcase;

final class ToBuilderValidator {

  private final ImmutableMap<String, ExecutableElement> methods;
  private final ImmutableMap<String, VariableElement> fields;
  private final GoalElement goal;
  private final Elements elements;

  private ToBuilderValidator(ImmutableMap<String, ExecutableElement> methodsByName,
                             ImmutableMap<String, VariableElement> fieldsByName,
                             GoalElement goal, Elements elements) {
    this.methods = methodsByName;
    this.fields = fieldsByName;
    this.goal = goal;
    this.elements = elements;
  }

  private static ToBuilderValidator create(TypeElement buildElement, GoalElement goal, Elements elements) {
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
    return new ToBuilderValidator(methodsByName, fieldsByName, goal, elements);
  }

  ImmutableList<ValidParameter> validate() throws ValidationException {
    return shuffledParameters(goal.accept(new Analyser.AbstractGoalElement.GoalElementCases<ImmutableList<TmpValidParameter>>() {
      @Override
      public ImmutableList<TmpValidParameter> executable(ExecutableElement goal, GoalKind kind) throws ValidationException {
        ImmutableList.Builder<TmpValidParameter> builder = ImmutableList.builder();
        for (VariableElement parameter : goal.getParameters()) {
          VariableElement field = fields.get(parameter.getSimpleName().toString());
          if (field != null && TypeName.get(field.asType()).equals(TypeName.get(parameter.asType()))) {
            builder.add(TmpValidParameter.create(parameter, Optional.<String>absent()));
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
            builder.add(TmpValidParameter.create(parameter, Optional.of(methodName)));
          }
        }
        return builder.build();
      }
      @Override
      public ImmutableList<TmpValidParameter> field(VariableElement field) throws ValidationException {
        TypeName typeName = TypeName.get(field.asType());
        TypeElement type = elements.getTypeElement(typeName.toString());
        ImmutableList<ExecutableElement> setters = setters(field, type);
        ImmutableMap<String, ExecutableElement> getters
            = FluentIterable.from(getLocalAndInheritedMethods(type, elements))
            .filter(new Predicate<ExecutableElement>() {
              @Override
              public boolean apply(ExecutableElement method) {
                return method.getParameters().isEmpty()
                    && method.getModifiers().contains(PUBLIC)
                    && !method.getModifiers().contains(STATIC)
                    && method.getSimpleName().toString().startsWith("get");
              }
            })
            .uniqueIndex(new Function<ExecutableElement, String>() {
              @Override
              public String apply(ExecutableElement method) {
                return method.getSimpleName().toString();
              }
            });
        ImmutableList.Builder<TmpValidParameter> builder = ImmutableList.builder();
        for (ExecutableElement setter : setters) {
          String getterName = "get" + setter.getSimpleName().toString().substring(3);
          ExecutableElement getter = getters.get(getterName);
          if (getter == null
              && TypeName.get(setter.getParameters().get(0).asType()) == TypeName.BOOLEAN) {
            getterName = "is" + setter.getSimpleName().toString().substring(3);
            getter = getters.get(getterName);
          }
          if (getter != null
              && getter.getKind() == ElementKind.METHOD
              && getter.getModifiers().contains(PUBLIC)
              && getter.getParameters().isEmpty()
              && getter.getReturnType().equals(setter.getParameters().get(0).asType())) {
            if (getter.getThrownTypes().isEmpty()) {
              builder.add(TmpValidParameter.create(setter, Optional.of(getterName)));
            } else {
              throw new ValidationException("Getter may not declare exception: " + getterName, field);
            }
          } else {
            throw new ValidationException("Could not find method "
                + getterName + "()", field);
          }
        }
        return builder.build();
      }
    }));
  }

  ImmutableList<ValidParameter> skip() throws ValidationException {
    return shuffledParameters(goal.accept(new Analyser.AbstractGoalElement.GoalElementCases<ImmutableList<TmpValidParameter>>() {
      @Override
      public ImmutableList<TmpValidParameter> executable(ExecutableElement goal, GoalKind kind) throws ValidationException {
        ImmutableList.Builder<TmpValidParameter> builder = ImmutableList.builder();
        for (VariableElement parameter : goal.getParameters()) {
          builder.add(TmpValidParameter.create(parameter, Optional.<String>absent()));
        }
        return builder.build();
      }
      @Override
      public ImmutableList<TmpValidParameter> field(VariableElement field) throws ValidationException {
        TypeName typeName = TypeName.get(field.asType());
        TypeElement type = elements.getTypeElement(typeName.toString());
        ImmutableList<ExecutableElement> setters = setters(field, type);
        ImmutableList.Builder<TmpValidParameter> builder = ImmutableList.builder();
        for (ExecutableElement setter : setters) {
          builder.add(TmpValidParameter.create(setter, Optional.<String>absent()));
        }
        return builder.build();
      }
    }));
  }

  private ImmutableList<ExecutableElement> setters(VariableElement field, TypeElement type) throws ValidationException {
    if (!parameterlessConstructor(type).isPresent()) {
      throw new ValidationException(NO_DEFAULT_CONSTRUCTOR, field);
    }
    if (!type.getModifiers().contains(PUBLIC)) {
      throw new ValidationException(TARGET_PUBLIC, field);
    }
    if (!ALLOWED_NESTING_KINDS.contains(type.getNestingKind())
        || type.getNestingKind() == MEMBER && !type.getModifiers().contains(STATIC)) {
      throw new ValidationException(TARGET_NESTING_KIND, type);
    }
    ImmutableList<ExecutableElement> methods = ImmutableList.copyOf(getLocalAndInheritedMethods(type, elements));
    ImmutableList.Builder<ExecutableElement> builder = ImmutableList.builder();
    for (int i = 0; i < methods.size(); i++) {
      ExecutableElement method = methods.get(i);
      if (method.getKind() == ElementKind.METHOD
          && method.getModifiers().contains(PUBLIC)
          && method.getSimpleName().length() >= 4
          && isUpperCase(method.getSimpleName().charAt(3))
          && method.getSimpleName().toString().startsWith("set")
          && method.getParameters().size() == 1
          && method.getReturnType().getKind() == TypeKind.VOID) {
        if (method.getThrownTypes().isEmpty()) {
          builder.add(method);
        } else {
          throw new ValidationException("Setter may not declare exception: " + method.getSimpleName(), field);
        }
      }
    }
    ImmutableList<ExecutableElement> setters = builder.build();
    if (setters.isEmpty()) {
      throw new ValidationException(NO_SETTERS, field);
    }
    return setters;
  }

  private Optional<ExecutableElement> parameterlessConstructor(TypeElement type) {
    List<ExecutableElement> constructors = ElementFilter.constructorsIn(type.getEnclosedElements());
    for (ExecutableElement constructor : constructors) {
      if (constructor.getParameters().isEmpty()
          && constructor.getModifiers().contains(PUBLIC)) {
        return Optional.of(constructor);
      }
    }
    return Optional.absent();
  }

  static final class Factory {
    private final Elements elements;

    Factory(Elements elements) {
      this.elements = elements;
    }
    Builder goalElement(GoalElement goalElement) {
      return new Builder().elements(elements).goalElement(goalElement);
    }
  }

  static final class Builder {
    private GoalElement goalElement;
    private Elements elements;
    private Builder goalElement(GoalElement goalElement) {
      this.goalElement = goalElement;
      return this;
    }
    private Builder elements(Elements elements) {
      this.elements = elements;
      return this;
    }
    ToBuilderValidator buildElement(TypeElement buildElement) {
      return create(buildElement, goalElement, elements);
    }
  }

  private static ImmutableList<ValidParameter> shuffledParameters(ImmutableList<TmpValidParameter> parameters)
      throws ValidationException {
    ValidParameter[] builder = new ValidParameter[parameters.size()];
    ImmutableList.Builder<TmpValidParameter> noAnnotation = ImmutableList.builder();
    for (TmpValidParameter parameter : parameters) {
      Optional<Step> step = parameter.annotation;
      if (step.isPresent()) {
        int value = step.get().value();
        if (value < 0) {
          throw new ValidationException(ERROR,
              NEGATIVE_STEP_POSITION, parameter.element);
        }
        if (value >= parameters.size()) {
          throw new ValidationException(ERROR,
              STEP_POSITION_TOO_LARGE, parameter.element);
        }
        if (builder[value] != null) {
          throw new ValidationException(ERROR,
              DUPLICATE_STEP_POSITION, parameter.element);
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

  private static final class TmpValidParameter {

    private final Element element;
    private final String name;
    private final TypeName type;
    private final Optional<Step> annotation;
    private final Optional<String> projectionMethodName;

    private TmpValidParameter(Element element, String name, TypeName type, Optional<Step> annotation, Optional<String> projectionMethodName) {
      this.element = element;
      this.name = name;
      this.type = type;
      this.annotation = annotation;
      this.projectionMethodName = projectionMethodName;
    }

    static TmpValidParameter create(VariableElement parameter, Optional<String> projectionMethodName) {
      return new TmpValidParameter(parameter,
          parameter.getSimpleName().toString(),
          TypeName.get(parameter.asType()),
          Optional.fromNullable(parameter.getAnnotation(Step.class)),
          projectionMethodName);
    }

    static TmpValidParameter create(ExecutableElement setter, Optional<String> projectionMethodName) {
      return new TmpValidParameter(setter,
          downcase(setter.getSimpleName().toString().substring(3)),
          TypeName.get(setter.getParameters().get(0).asType()),
          Optional.fromNullable(setter.getAnnotation(Step.class)),
          projectionMethodName);
    }

    private ValidParameter toValidParameter() {
      return new ValidParameter(name, type, projectionMethodName);
    }
  }

  static final class ValidParameter {

    final String name;
    final TypeName type;

    /**
     * <p>method, constructor goal: getter name (absence -> !toBuilder or field access)</p>
     * <p>field goal: getter (absence -> !toBuilder)</p>
     */
    final Optional<String> projectionMethodName;


    ValidParameter(String name, TypeName type, Optional<String> projectionMethodName) {
      this.name = name;
      this.type = type;
      this.projectionMethodName = projectionMethodName;
    }
  }

}
