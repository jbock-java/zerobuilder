package net.zerobuilder.compiler;

import com.google.auto.common.MoreTypes;
import com.google.common.base.Function;
import com.google.common.base.Optional;
import com.google.common.base.Predicate;
import com.google.common.collect.FluentIterable;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Maps;
import com.squareup.javapoet.ClassName;
import com.squareup.javapoet.TypeName;
import net.zerobuilder.Step;
import net.zerobuilder.compiler.Analyser.AbstractGoalElement.GoalElementCases;
import net.zerobuilder.compiler.Analyser.GoalElement;
import net.zerobuilder.compiler.GoalContextFactory.GoalKind;

import javax.lang.model.element.Element;
import javax.lang.model.element.ElementKind;
import javax.lang.model.element.ExecutableElement;
import javax.lang.model.element.TypeElement;
import javax.lang.model.element.VariableElement;
import javax.lang.model.type.TypeKind;
import javax.lang.model.type.TypeMirror;
import javax.lang.model.util.ElementFilter;
import javax.lang.model.util.Elements;
import java.util.Collection;
import java.util.List;

import static com.google.auto.common.MoreElements.getLocalAndInheritedMethods;
import static com.google.auto.common.MoreTypes.asTypeElement;
import static com.google.common.base.Ascii.isUpperCase;
import static javax.lang.model.element.Modifier.PRIVATE;
import static javax.lang.model.element.Modifier.PUBLIC;
import static javax.lang.model.element.Modifier.STATIC;
import static javax.lang.model.element.NestingKind.MEMBER;
import static javax.lang.model.util.ElementFilter.fieldsIn;
import static javax.lang.model.util.ElementFilter.methodsIn;
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

  private final GoalElement goal;
  private final Elements elements;

  private final GoalElementCases<ImmutableList<TmpValidParameter>> validate = new GoalElementCases<ImmutableList<TmpValidParameter>>() {
    @Override
    public ImmutableList<TmpValidParameter> executable(ExecutableElement goal, GoalKind kind) throws ValidationException {
      TypeElement type = asTypeElement(goal.getEnclosingElement().asType());
      ImmutableMap<String, ExecutableElement> methods = FluentIterable.from(getLocalAndInheritedMethods(type, elements))
          .filter(new Predicate<ExecutableElement>() {
            @Override
            public boolean apply(ExecutableElement method) {
              return method.getParameters().isEmpty()
                  && !method.getModifiers().contains(PRIVATE)
                  && !method.getModifiers().contains(STATIC);
            }
          })
          .uniqueIndex(new Function<ExecutableElement, String>() {
            @Override
            public String apply(ExecutableElement method) {
              return method.getSimpleName().toString();
            }
          });
      ImmutableMap<String, VariableElement> fields
          = FluentIterable.from(fieldsIn(type.getEnclosedElements()))
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
    public ImmutableList<TmpValidParameter> field(Element fieldOrType, TypeElement typeElement) throws ValidationException {
      return validateBean(fieldOrType, typeElement);
    }
  };

  private static ImmutableList<TmpValidParameter> validateBean(Element fieldOrType, TypeElement typeElement) throws ValidationException {
    ImmutableMap<String, ExecutableElement> setters = setters(fieldOrType, typeElement);
    ImmutableList<ExecutableElement> getters
        = FluentIterable.from(methodsIn(typeElement.getEnclosedElements()))
        .filter(new Predicate<ExecutableElement>() {
          @Override
          public boolean apply(ExecutableElement method) {
            String name = method.getSimpleName().toString();
            return method.getParameters().isEmpty()
                && method.getModifiers().contains(PUBLIC)
                && !method.getModifiers().contains(STATIC)
                && !method.getReturnType().getKind().equals(TypeKind.VOID)
                && !method.getReturnType().getKind().equals(TypeKind.NONE)
                && (name.startsWith("get") || name.startsWith("is"));
          }
        })
        .toList();
    ImmutableList.Builder<TmpValidParameter> builder = ImmutableList.builder();
    for (ExecutableElement getter : getters) {
      String name = getter.getSimpleName().toString();
      String setterName = name.substring(name.startsWith("get") ? 3 : 2);
      ExecutableElement setter = setters.get(setterName);
      if (setter != null) {
        if (!setter.getParameters().get(0).asType().equals(getter.getReturnType())) {
          throw new ValidationException("Getter/setter type mismatch", setter);
        }
        if (!getter.getThrownTypes().isEmpty()) {
          throw new ValidationException("Getter may not declare exception", getter);
        }
        builder.add(TmpValidParameter.create(getter, Optional.<ClassName>absent()));
      } else if (isCollection(getter.getReturnType())) {
        ImmutableSet<TypeElement> referenced = MoreTypes.referencedTypes(getter.getReturnType());
        if (referenced.size() == 1) {
          builder.add(TmpValidParameter.create(getter, Optional.of(ClassName.get(Object.class))));
        } else if (referenced.size() == 2) {
          boolean found = false;
          for (TypeElement element : referenced) {
              if (!isCollection(element.asType())) {
              ClassName generic = ClassName.get(element);
              builder.add(TmpValidParameter.create(getter, Optional.of(generic)));
              found = true;
            }
          }
          if (!found) {
            throw new ValidationException("Can't understand the generics of this field", fieldOrType);
          }
        } else {
          throw new ValidationException("Can't understand the generics of this field", fieldOrType);
        }
      } else {
        throw new ValidationException("Could not find setter set"
            + setterName + "(...)", fieldOrType);
      }
    }
    return builder.build();
  }

  private static boolean isCollection(TypeMirror typeMirror) {
    if (!typeMirror.getKind().equals(TypeKind.DECLARED)) {
      return false;
    }
    TypeElement element = asTypeElement(typeMirror);
    TypeName className = ClassName.get(element);
    if (className.equals(ClassName.get(Collection.class))) {
      return true;
    }
    if (className.equals(ClassName.get(Object.class))) {
      return false;
    }
    for (TypeMirror anInterface : element.getInterfaces()) {
      if (isCollection(anInterface)) {
        return true;
      }
    }
    return false;
  }

  private static final GoalElementCases<ImmutableList<TmpValidParameter>> skip = new GoalElementCases<ImmutableList<TmpValidParameter>>() {
    @Override
    public ImmutableList<TmpValidParameter> executable(ExecutableElement goal, GoalKind kind) throws ValidationException {
      ImmutableList.Builder<TmpValidParameter> builder = ImmutableList.builder();
      for (VariableElement parameter : goal.getParameters()) {
        builder.add(TmpValidParameter.create(parameter, Optional.<String>absent()));
      }
      return builder.build();
    }
    @Override
    public ImmutableList<TmpValidParameter> field(Element fieldOrType, TypeElement typeElement) throws ValidationException {
      return validateBean(fieldOrType, typeElement);
    }
  };

  private ToBuilderValidator(GoalElement goal, Elements elements) {
    this.goal = goal;
    this.elements = elements;
  }

  private static ToBuilderValidator create(GoalElement goal, Elements elements) {
    return new ToBuilderValidator(goal, elements);
  }

  ImmutableList<ValidParameter> validate() throws ValidationException {
    return shuffledParameters(goal.accept(validate));
  }

  ImmutableList<ValidParameter> skip() throws ValidationException {
    return shuffledParameters(goal.accept(skip));
  }

  private static ImmutableMap<String, ExecutableElement> setters(Element field, TypeElement type) throws ValidationException {
    if (!hasParameterlessConstructor(type)) {
      throw new ValidationException(NO_DEFAULT_CONSTRUCTOR + TypeName.get(type.asType()), field);
    }
    if (!type.getModifiers().contains(PUBLIC)) {
      throw new ValidationException(TARGET_PUBLIC, field);
    }
    if (!ALLOWED_NESTING_KINDS.contains(type.getNestingKind())
        || type.getNestingKind() == MEMBER && !type.getModifiers().contains(STATIC)) {
      throw new ValidationException(TARGET_NESTING_KIND, type);
    }
    ImmutableList<ExecutableElement> methods = ImmutableList.copyOf(methodsIn(type.getEnclosedElements()));
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
    return Maps.uniqueIndex(builder.build(), new Function<ExecutableElement, String>() {
      @Override
      public String apply(ExecutableElement setter) {
        return setter.getSimpleName().toString().substring(3);
      }
    });
  }

  private static boolean hasParameterlessConstructor(TypeElement type) {
    List<ExecutableElement> constructors = ElementFilter.constructorsIn(type.getEnclosedElements());
    for (ExecutableElement constructor : constructors) {
      if (constructor.getParameters().isEmpty()
          && constructor.getModifiers().contains(PUBLIC)) {
        return true;
      }
    }
    return false;
  }

  static final class Factory {
    private final Elements elements;

    Factory(Elements elements) {
      this.elements = elements;
    }
    ToBuilderValidator goalElement(GoalElement goalElement) {
      return create(goalElement, elements);
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
    private final Optional<ClassName> setterlessCollection;
    private final Optional<Step> annotation;
    private final Optional<String> projectionMethodName;

    private TmpValidParameter(Element element, String name, TypeName type, Optional<ClassName> setterlessCollection,
                              Optional<Step> annotation, Optional<String> projectionMethodName) {
      this.element = element;
      this.name = name;
      this.type = type;
      this.setterlessCollection = setterlessCollection;
      this.annotation = annotation;
      this.projectionMethodName = projectionMethodName;
    }

    static TmpValidParameter create(VariableElement parameter, Optional<String> projectionMethodName) {
      return new TmpValidParameter(parameter,
          parameter.getSimpleName().toString(),
          TypeName.get(parameter.asType()),
          Optional.<ClassName>absent(), Optional.fromNullable(parameter.getAnnotation(Step.class)),
          projectionMethodName);
    }

    static TmpValidParameter create(ExecutableElement getter, Optional<ClassName> setterlessCollection) {
      String name = getter.getSimpleName().toString();
      return new TmpValidParameter(getter,
          downcase(name.substring(name.startsWith("get") ? 3 : 2)),
          TypeName.get(getter.getReturnType()),
          setterlessCollection, Optional.fromNullable(getter.getAnnotation(Step.class)),
          Optional.of(name));
    }

    private ValidParameter toValidParameter() {
      return new ValidParameter(name, type, setterlessCollection, projectionMethodName);
    }
  }

  static final class ValidParameter {

    final String name;
    final TypeName type;

    /**
     * Only beans can have this. For {@code List<String>} this would be {@code String}.
     */
    final Optional<ClassName> setterlessCollection;

    /**
     * <p>method, constructor goal: getter name (absence -> !toBuilder or field access)</p>
     * <p>field goal: getter (absence -> !toBuilder)</p>
     */
    final Optional<String> projectionMethodName;


    ValidParameter(String name, TypeName type, Optional<ClassName> setterlessCollection, Optional<String> projectionMethodName) {
      this.name = name;
      this.type = type;
      this.setterlessCollection = setterlessCollection;
      this.projectionMethodName = projectionMethodName;
    }
  }

}
