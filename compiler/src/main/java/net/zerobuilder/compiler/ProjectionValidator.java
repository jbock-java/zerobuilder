package net.zerobuilder.compiler;

import com.google.auto.common.MoreTypes;
import com.google.common.base.Function;
import com.google.common.base.Optional;
import com.google.common.base.Predicate;
import com.google.common.collect.FluentIterable;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Ordering;
import com.squareup.javapoet.ClassName;
import com.squareup.javapoet.TypeName;
import net.zerobuilder.Step;
import net.zerobuilder.compiler.Analyser.AbstractGoalElement.GoalElementCases;
import net.zerobuilder.compiler.ProjectionValidator.TmpValidParameter.AccessorPairTmpValidParameter;
import net.zerobuilder.compiler.ProjectionValidator.ValidParameter.AccessorPair;
import net.zerobuilder.compiler.ProjectionValidator.ValidParameter.Parameter;

import javax.lang.model.element.Element;
import javax.lang.model.element.ElementKind;
import javax.lang.model.element.ExecutableElement;
import javax.lang.model.element.TypeElement;
import javax.lang.model.element.VariableElement;
import javax.lang.model.type.TypeKind;
import javax.lang.model.type.TypeMirror;
import javax.lang.model.util.ElementFilter;
import javax.lang.model.util.Elements;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.List;

import static com.google.auto.common.MoreElements.getLocalAndInheritedMethods;
import static com.google.auto.common.MoreTypes.asTypeElement;
import static com.google.common.base.Ascii.isUpperCase;
import static com.google.common.base.Optional.fromNullable;
import static com.google.common.collect.Maps.uniqueIndex;
import static java.util.Collections.nCopies;
import static javax.lang.model.element.Modifier.PRIVATE;
import static javax.lang.model.element.Modifier.PUBLIC;
import static javax.lang.model.element.Modifier.STATIC;
import static javax.lang.model.element.NestingKind.MEMBER;
import static javax.lang.model.util.ElementFilter.fieldsIn;
import static javax.lang.model.util.ElementFilter.methodsIn;
import static javax.tools.Diagnostic.Kind.ERROR;
import static net.zerobuilder.compiler.Messages.ErrorMessages.BAD_GENERICS;
import static net.zerobuilder.compiler.Messages.ErrorMessages.COULD_NOT_FIND_SETTER;
import static net.zerobuilder.compiler.Messages.ErrorMessages.DUPLICATE_STEP_POSITION;
import static net.zerobuilder.compiler.Messages.ErrorMessages.GETTER_EXCEPTION;
import static net.zerobuilder.compiler.Messages.ErrorMessages.GETTER_SETTER_TYPE_MISMATCH;
import static net.zerobuilder.compiler.Messages.ErrorMessages.NEGATIVE_STEP_POSITION;
import static net.zerobuilder.compiler.Messages.ErrorMessages.NO_DEFAULT_CONSTRUCTOR;
import static net.zerobuilder.compiler.Messages.ErrorMessages.NO_PROJECTION;
import static net.zerobuilder.compiler.Messages.ErrorMessages.SETTER_EXCEPTION;
import static net.zerobuilder.compiler.Messages.ErrorMessages.STEP_POSITION_TOO_LARGE;
import static net.zerobuilder.compiler.Messages.ErrorMessages.TARGET_NESTING_KIND;
import static net.zerobuilder.compiler.Messages.ErrorMessages.TARGET_PUBLIC;
import static net.zerobuilder.compiler.TypeValidator.ALLOWED_NESTING_KINDS;
import static net.zerobuilder.compiler.Utilities.downcase;
import static net.zerobuilder.compiler.Utilities.upcase;

final class ProjectionValidator {

  private static final Ordering<AccessorPairTmpValidParameter> ACCESSOR_PAIR_ORDERING
      = Ordering.from(new Comparator<AccessorPairTmpValidParameter>() {
    @Override
    public int compare(AccessorPairTmpValidParameter pair0, AccessorPairTmpValidParameter pair1) {
      return pair0.accessorPair.name.compareTo(pair1.accessorPair.name);
    }
  });

  private final Elements elements;

  final GoalElementCases<ValidationResult> validate = new GoalElementCases<ValidationResult>() {
    @Override
    public ValidationResult executableGoal(Analyser.ExecutableGoal goal) throws ValidationException {
      TypeElement type = asTypeElement(goal.executableElement.getEnclosingElement().asType());
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

      ImmutableList.Builder<TmpValidParameter.RegularTmpValidParameter> builder = ImmutableList.builder();
      for (VariableElement parameter : goal.executableElement.getParameters()) {
        VariableElement field = fields.get(parameter.getSimpleName().toString());
        if (field != null && TypeName.get(field.asType()).equals(TypeName.get(parameter.asType()))) {
          builder.add(TmpValidParameter.RegularTmpValidParameter.create(parameter, Optional.<String>absent()));
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
            throw new ValidationException(NO_PROJECTION, parameter);
          }
          builder.add(TmpValidParameter.RegularTmpValidParameter.create(parameter, Optional.of(methodName)));
        }
      }
      ImmutableList<TmpValidParameter.RegularTmpValidParameter> shuffled = shuffledParameters(builder.build());
      return new ValidationResult.RegularValidationResult(goal,
          FluentIterable.from(shuffled).transform(TmpValidParameter.RegularTmpValidParameter.toValidParameter).toList());
    }
    @Override
    public ValidationResult beanGoal(Analyser.BeanGoal goal) throws ValidationException {
      return validateBean(goal);
    }
  };

  private static ValidationResult validateBean(Analyser.BeanGoal goal) throws ValidationException {
    ImmutableMap<String, ExecutableElement> setters = setters(goal.beanTypeElement);
    ImmutableList<ExecutableElement> getters
        = FluentIterable.from(methodsIn(goal.beanTypeElement.getEnclosedElements()))
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
    ImmutableList.Builder<AccessorPairTmpValidParameter> builder = ImmutableList.builder();
    for (ExecutableElement getter : getters) {
      String name = getter.getSimpleName().toString();
      String setterName = name.substring(name.startsWith("get") ? 3 : 2);
      ExecutableElement setter = setters.get(setterName);
      if (setter != null) {
        if (!setter.getParameters().get(0).asType().equals(getter.getReturnType())) {
          throw new ValidationException(GETTER_SETTER_TYPE_MISMATCH, setter);
        }
        if (!getter.getThrownTypes().isEmpty()) {
          throw new ValidationException(GETTER_EXCEPTION, getter);
        }
        builder.add(AccessorPairTmpValidParameter.create(getter, Optional.<ClassName>absent()));
      } else if (isCollection(getter.getReturnType())) {
        ImmutableSet<TypeElement> referenced = MoreTypes.referencedTypes(getter.getReturnType());
        if (referenced.size() == 1) {
          builder.add(AccessorPairTmpValidParameter.create(getter, Optional.of(ClassName.get(Object.class))));
        } else if (referenced.size() == 2) {
          boolean found = false;
          for (TypeElement element : referenced) {
            if (!isCollection(element.asType())) {
              ClassName generic = ClassName.get(element);
              builder.add(AccessorPairTmpValidParameter.create(getter, Optional.of(generic)));
              found = true;
            }
          }
          if (!found) {
            throw new ValidationException(BAD_GENERICS, getter);
          }
        } else {
          throw new ValidationException(BAD_GENERICS, getter);
        }
      } else {
        throw new ValidationException(COULD_NOT_FIND_SETTER, getter);
      }
    }
    ImmutableList<AccessorPairTmpValidParameter> parameters = ACCESSOR_PAIR_ORDERING.immutableSortedCopy(builder.build());
    ImmutableList<AccessorPairTmpValidParameter> shuffled = shuffledParameters(
        parameters);
    return new ValidationResult.BeanValidationResult(goal,
        FluentIterable.from(shuffled).transform(AccessorPairTmpValidParameter.toValidParameter).toList());
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

  static final GoalElementCases<ValidationResult> skip = new GoalElementCases<ValidationResult>() {
    @Override
    public ValidationResult executableGoal(Analyser.ExecutableGoal goal) throws ValidationException {
      ImmutableList.Builder<TmpValidParameter.RegularTmpValidParameter> builder = ImmutableList.builder();
      for (VariableElement parameter : goal.executableElement.getParameters()) {
        builder.add(TmpValidParameter.RegularTmpValidParameter.create(parameter, Optional.<String>absent()));
      }
      ImmutableList<TmpValidParameter.RegularTmpValidParameter> shuffled = shuffledParameters(builder.build());
      return new ValidationResult.RegularValidationResult(goal,
          FluentIterable.from(shuffled).transform(TmpValidParameter.RegularTmpValidParameter.toValidParameter).toList());
    }
    @Override
    public ValidationResult beanGoal(Analyser.BeanGoal goal) throws ValidationException {
      return validateBean(goal);
    }
  };

  private ProjectionValidator(Elements elements) {
    this.elements = elements;
  }

  static ProjectionValidator create(Elements elements) {
    return new ProjectionValidator(elements);
  }

  private static ImmutableMap<String, ExecutableElement> setters(TypeElement beanType) throws ValidationException {
    if (!hasParameterlessConstructor(beanType)) {
      throw new ValidationException(NO_DEFAULT_CONSTRUCTOR + TypeName.get(beanType.asType()), beanType);
    }
    if (!beanType.getModifiers().contains(PUBLIC)) {
      throw new ValidationException(TARGET_PUBLIC, beanType);
    }
    if (!ALLOWED_NESTING_KINDS.contains(beanType.getNestingKind())
        || beanType.getNestingKind() == MEMBER && !beanType.getModifiers().contains(STATIC)) {
      throw new ValidationException(TARGET_NESTING_KIND, beanType);
    }
    ImmutableList<ExecutableElement> methods = ImmutableList.copyOf(methodsIn(beanType.getEnclosedElements()));
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
          throw new ValidationException(SETTER_EXCEPTION, method);
        }
      }
    }
    return uniqueIndex(builder.build(), new Function<ExecutableElement, String>() {
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

  private static <E extends TmpValidParameter> ImmutableList<E> shuffledParameters(ImmutableList<E> parameters)
      throws ValidationException {
    List<E> builder = new ArrayList(nCopies(parameters.size(), null));
    ImmutableList.Builder<E> noAnnotation = ImmutableList.builder();
    for (E parameter : parameters) {
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
        if (builder.get(value) != null) {
          throw new ValidationException(ERROR,
              DUPLICATE_STEP_POSITION, parameter.element);
        }
        builder.set(value, parameter);
      } else {
        noAnnotation.add(parameter);
      }
    }
    int pos = 0;
    for (E parameter : noAnnotation.build()) {
      while (builder.get(pos) != null) {
        pos++;
      }
      builder.set(pos++, parameter);
    }
    return ImmutableList.copyOf(builder);
  }

  abstract static class TmpValidParameter {

    final Element element;
    final Optional<Step> annotation;

    final static class RegularTmpValidParameter extends TmpValidParameter {
      private final Parameter parameter;
      private RegularTmpValidParameter(Element element, Optional<Step> annotation, Parameter parameter) {
        super(element, annotation);
        this.parameter = parameter;
      }
      static final Function<RegularTmpValidParameter, Parameter> toValidParameter = new Function<RegularTmpValidParameter, Parameter>() {
        @Override
        public Parameter apply(RegularTmpValidParameter parameter) {
          return parameter.parameter;
        }
      };
      static RegularTmpValidParameter create(VariableElement element, Optional<String> projectionMethodName) {
        Parameter parameter = new Parameter(element.getSimpleName().toString(), TypeName.get(element.asType()), projectionMethodName);
        Optional<Step> annotation = fromNullable(element.getAnnotation(Step.class));
        return new RegularTmpValidParameter(element, annotation, parameter);
      }
    }

    static final class AccessorPairTmpValidParameter extends TmpValidParameter {
      private final AccessorPair accessorPair;
      private AccessorPairTmpValidParameter(Element element, Optional<Step> annotation, AccessorPair accessorPair) {
        super(element, annotation);
        this.accessorPair = accessorPair;
      }
      static final Function<AccessorPairTmpValidParameter, AccessorPair> toValidParameter = new Function<AccessorPairTmpValidParameter, AccessorPair>() {
        @Override
        public AccessorPair apply(AccessorPairTmpValidParameter parameter) {
          return parameter.accessorPair;
        }
      };
      static AccessorPairTmpValidParameter create(ExecutableElement getter, Optional<ClassName> setterlessCollection) {
        AccessorPair accessorPair = new AccessorPair(TypeName.get(getter.getReturnType()), getter.getSimpleName().toString(), setterlessCollection);
        return new AccessorPairTmpValidParameter(getter, Optional.fromNullable(getter.getAnnotation(Step.class)), accessorPair);
      }
    }

    private TmpValidParameter(Element element, Optional<Step> annotation) {
      this.element = element;
      this.annotation = annotation;
    }

  }

  abstract static class ValidParameter {

    final String name;
    final TypeName type;

    static final class Parameter extends ValidParameter {
      final Optional<String> projectionMethodName;
      Parameter(String name, TypeName type, Optional<String> projectionMethodName) {
        super(name, type);
        this.projectionMethodName = projectionMethodName;
      }
    }

    static final class AccessorPair extends ValidParameter {
      final String projectionMethodName;

      /**
       * Only present if there is no setter for the collection.
       * If {@link #type} is {@code List<String>}, this would be {@code String}
       */
      final Optional<ClassName> collectionType;

      AccessorPair(TypeName type, String projectionMethodName, Optional<ClassName> collectionType) {
        super(name(projectionMethodName), type);
        this.projectionMethodName = projectionMethodName;
        this.collectionType = collectionType;
      }

      private static String name(String projectionMethodName) {
        return downcase(projectionMethodName.substring(projectionMethodName.startsWith("is") ? 2 : 3));
      }
    }

    ValidParameter(String name, TypeName type) {
      this.name = name;
      this.type = type;
    }
  }

  static abstract class ValidationResult {
    static abstract class ValidationResultCases<R> {
      abstract R executableGoal(Analyser.ExecutableGoal goal, ImmutableList<Parameter> parameters);
      abstract R beanGoal(Analyser.BeanGoal beanGoal, ImmutableList<AccessorPair> accessorPairs);
    }
    abstract <R> R accept(ValidationResultCases<R> cases);
    static final class RegularValidationResult extends ValidationResult {
      private final Analyser.ExecutableGoal goal;
      private final ImmutableList<Parameter> parameters;
      RegularValidationResult(Analyser.ExecutableGoal goal, ImmutableList<Parameter> parameters) {
        this.goal = goal;
        this.parameters = parameters;
      }
      @Override
      <R> R accept(ValidationResultCases<R> cases) {
        return cases.executableGoal(goal, parameters);
      }
    }

    static final class BeanValidationResult extends ValidationResult {
      private final Analyser.BeanGoal goal;
      private final ImmutableList<AccessorPair> accessorPairs;
      BeanValidationResult(Analyser.BeanGoal goal, ImmutableList<AccessorPair> accessorPairs) {
        this.goal = goal;
        this.accessorPairs = accessorPairs;
      }
      @Override
      <R> R accept(ValidationResultCases<R> cases) {
        return cases.beanGoal(goal, accessorPairs);
      }
    }
  }
}
