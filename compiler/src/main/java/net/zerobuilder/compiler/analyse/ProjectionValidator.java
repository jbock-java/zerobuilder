package net.zerobuilder.compiler.analyse;

import com.google.common.base.Function;
import com.google.common.base.Optional;
import com.google.common.base.Predicate;
import com.google.common.collect.FluentIterable;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Ordering;
import com.squareup.javapoet.ClassName;
import com.squareup.javapoet.ParameterizedTypeName;
import com.squareup.javapoet.TypeName;
import net.zerobuilder.Step;
import net.zerobuilder.compiler.analyse.Analyser.AbstractGoalElement.GoalElementCases;
import net.zerobuilder.compiler.analyse.Analyser.BeanGoal;
import net.zerobuilder.compiler.analyse.ProjectionValidator.TmpValidParameter.TmpAccessorPair;
import net.zerobuilder.compiler.analyse.ProjectionValidator.ValidParameter.AccessorPair;
import net.zerobuilder.compiler.analyse.ProjectionValidator.ValidParameter.RegularParameter;
import net.zerobuilder.compiler.analyse.ProjectionValidator.ValidationResult.BeanValidationResult;

import javax.lang.model.element.Element;
import javax.lang.model.element.ElementKind;
import javax.lang.model.element.ExecutableElement;
import javax.lang.model.element.TypeElement;
import javax.lang.model.element.VariableElement;
import javax.lang.model.type.TypeKind;
import javax.lang.model.type.TypeMirror;
import javax.lang.model.util.ElementFilter;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.List;

import static com.google.auto.common.MoreElements.getLocalAndInheritedMethods;
import static com.google.auto.common.MoreTypes.asDeclared;
import static com.google.auto.common.MoreTypes.asTypeElement;
import static com.google.common.base.Ascii.isUpperCase;
import static com.google.common.base.Optional.fromNullable;
import static com.google.common.collect.Iterables.getOnlyElement;
import static com.google.common.collect.Maps.uniqueIndex;
import static java.util.Collections.nCopies;
import static javax.lang.model.element.Modifier.PRIVATE;
import static javax.lang.model.element.Modifier.PUBLIC;
import static javax.lang.model.element.Modifier.STATIC;
import static javax.lang.model.util.ElementFilter.fieldsIn;
import static javax.tools.Diagnostic.Kind.ERROR;
import static net.zerobuilder.compiler.Utilities.downcase;
import static net.zerobuilder.compiler.Utilities.upcase;
import static net.zerobuilder.compiler.analyse.Messages.ErrorMessages.BAD_GENERICS;
import static net.zerobuilder.compiler.analyse.Messages.ErrorMessages.COULD_NOT_FIND_SETTER;
import static net.zerobuilder.compiler.analyse.Messages.ErrorMessages.DUPLICATE_STEP_POSITION;
import static net.zerobuilder.compiler.analyse.Messages.ErrorMessages.GETTER_EXCEPTION;
import static net.zerobuilder.compiler.analyse.Messages.ErrorMessages.GETTER_SETTER_TYPE_MISMATCH;
import static net.zerobuilder.compiler.analyse.Messages.ErrorMessages.NO_DEFAULT_CONSTRUCTOR;
import static net.zerobuilder.compiler.analyse.Messages.ErrorMessages.NO_PROJECTION;
import static net.zerobuilder.compiler.analyse.Messages.ErrorMessages.SETTER_EXCEPTION;
import static net.zerobuilder.compiler.analyse.Messages.ErrorMessages.STEP_POSITION_TOO_LARGE;
import static net.zerobuilder.compiler.analyse.Messages.ErrorMessages.TARGET_PUBLIC;
import static net.zerobuilder.compiler.analyse.ProjectionValidator.TmpValidParameter.TmpAccessorPair.toValidParameter;

public final class ProjectionValidator {

  private static final Ordering<TmpAccessorPair> ACCESSOR_PAIR_ORDERING
      = Ordering.from(new Comparator<TmpAccessorPair>() {
    @Override
    public int compare(TmpAccessorPair pair0, TmpAccessorPair pair1) {
      return pair0.accessorPair.name.compareTo(pair1.accessorPair.name);
    }
  });

  static final GoalElementCases<ValidationResult> validate = new GoalElementCases<ValidationResult>() {
    @Override
    public ValidationResult executableGoal(Analyser.ExecutableGoal goal) throws ValidationException {
      TypeElement type = asTypeElement(goal.executableElement.getEnclosingElement().asType());
      ImmutableMap<String, ExecutableElement> methods = FluentIterable.from(getLocalAndInheritedMethods(type, goal.elements))
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

      ImmutableList.Builder<TmpValidParameter.TmpRegularParameter> builder = ImmutableList.builder();
      for (VariableElement parameter : goal.executableElement.getParameters()) {
        VariableElement field = fields.get(parameter.getSimpleName().toString());
        if (field != null && TypeName.get(field.asType()).equals(TypeName.get(parameter.asType()))) {
          builder.add(TmpValidParameter.TmpRegularParameter.create(parameter, Optional.<String>absent()));
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
          builder.add(TmpValidParameter.TmpRegularParameter.create(parameter, Optional.of(methodName)));
        }
      }
      ImmutableList<TmpValidParameter.TmpRegularParameter> shuffled = shuffledParameters(builder.build());
      return new ValidationResult.RegularValidationResult(goal,
          FluentIterable.from(shuffled).transform(TmpValidParameter.TmpRegularParameter.toValidParameter).toList());
    }
    @Override
    public ValidationResult beanGoal(BeanGoal goal) throws ValidationException {
      return validateBean(goal);
    }
  };

  private static ValidationResult validateBean(BeanGoal goal) throws ValidationException {
    ImmutableMap<String, ExecutableElement> setters = setters(goal);
    ImmutableList.Builder<TmpAccessorPair> builder = ImmutableList.builder();
    for (ExecutableElement getter : getters(goal)) {
      String name = getter.getSimpleName().toString();
      String setterName = name.substring(name.startsWith("get") ? 3 : 2);
      ExecutableElement setter = setters.get(setterName);
      if (setter != null) {
        TypeName setterType = TypeName.get(setter.getParameters().get(0).asType());
        TypeName getterType = TypeName.get(getter.getReturnType());
        if (!setterType.equals(getterType)) {
          throw new ValidationException(GETTER_SETTER_TYPE_MISMATCH, setter);
        }
        if (!getter.getThrownTypes().isEmpty()) {
          throw new ValidationException(GETTER_EXCEPTION, getter);
        }
        builder.add(TmpAccessorPair.create(getter, CollectionType.absent));
      } else if (isImplementationOf(getter.getReturnType(), ClassName.get(Collection.class))) {
        // no setter but we have a getter that returns something like List<E>
        // in this case we need to find what E is ("collectionType")
        List<? extends TypeMirror> referenced = asDeclared(getter.getReturnType()).getTypeArguments();
        if (referenced.isEmpty()) {
          // raw collection
          ClassName collectionType = ClassName.get(Object.class);
          builder.add(TmpAccessorPair.create(getter, CollectionType.of(collectionType, false)));
        } else if (referenced.size() == 1) {
          // one type parameter
          TypeMirror collectionType = getOnlyElement(referenced);
          boolean allowShortcut = !ClassName.get(asTypeElement(collectionType)).equals(ParameterizedTypeName.get(Iterable.class));
          builder.add(TmpAccessorPair.create(getter, CollectionType.of(ParameterizedTypeName.get(collectionType), allowShortcut)));
        } else {
          // unlikely: Collection should not have more than one type parameter
          throw new ValidationException(BAD_GENERICS, getter);
        }
      } else {
        throw new ValidationException(COULD_NOT_FIND_SETTER, getter);
      }
    }
    ImmutableList<TmpAccessorPair> parameters
        = shuffledParameters(ACCESSOR_PAIR_ORDERING.immutableSortedCopy(builder.build()));
    return new BeanValidationResult(goal,
        FluentIterable.from(parameters).transform(toValidParameter).toList());
  }

  private static boolean isImplementationOf(TypeMirror typeMirror, ClassName test) {
    if (!typeMirror.getKind().equals(TypeKind.DECLARED)) {
      return false;
    }
    TypeElement element = asTypeElement(typeMirror);
    TypeName className = ClassName.get(element);
    if (className.equals(test)) {
      return true;
    }
    if (className.equals(ClassName.get(Object.class))) {
      return false;
    }
    for (TypeMirror anInterface : element.getInterfaces()) {
      if (isImplementationOf(anInterface, test)) {
        return true;
      }
    }
    return false;
  }

  static final GoalElementCases<ValidationResult> skip = new GoalElementCases<ValidationResult>() {
    @Override
    public ValidationResult executableGoal(Analyser.ExecutableGoal goal) throws ValidationException {
      ImmutableList.Builder<TmpValidParameter.TmpRegularParameter> builder = ImmutableList.builder();
      for (VariableElement parameter : goal.executableElement.getParameters()) {
        builder.add(TmpValidParameter.TmpRegularParameter.create(parameter, Optional.<String>absent()));
      }
      ImmutableList<TmpValidParameter.TmpRegularParameter> shuffled = shuffledParameters(builder.build());
      return new ValidationResult.RegularValidationResult(goal,
          FluentIterable.from(shuffled).transform(TmpValidParameter.TmpRegularParameter.toValidParameter).toList());
    }
    @Override
    public ValidationResult beanGoal(BeanGoal goal) throws ValidationException {
      return validateBean(goal);
    }
  };

  private static ImmutableList<ExecutableElement> getters(BeanGoal goal) {
    return FluentIterable.from(getLocalAndInheritedMethods(goal.beanTypeElement, goal.elements))
        .filter(new Predicate<ExecutableElement>() {
          @Override
          public boolean apply(ExecutableElement method) {
            String name = method.getSimpleName().toString();
            return method.getParameters().isEmpty()
                && method.getModifiers().contains(PUBLIC)
                && !method.getModifiers().contains(STATIC)
                && !method.getReturnType().getKind().equals(TypeKind.VOID)
                && !method.getReturnType().getKind().equals(TypeKind.NONE)
                && (name.startsWith("get") || name.startsWith("is"))
                && !"getClass".equals(name);
          }
        })
        .toList();
  }

  private static ImmutableMap<String, ExecutableElement> setters(BeanGoal goal) throws ValidationException {
    TypeElement beanType = goal.beanTypeElement;
    if (!hasParameterlessConstructor(beanType)) {
      throw new ValidationException(NO_DEFAULT_CONSTRUCTOR, beanType);
    }
    if (!beanType.getModifiers().contains(PUBLIC)) {
      throw new ValidationException(TARGET_PUBLIC, beanType);
    }
    ImmutableList<ExecutableElement> methods = ImmutableList.copyOf(getLocalAndInheritedMethods(beanType, goal.elements));
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
      if (step.isPresent() && step.get().value() >= 0) {
        int value = step.get().value();
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

    final static class TmpRegularParameter extends TmpValidParameter {
      private final RegularParameter parameter;
      private TmpRegularParameter(Element element, Optional<Step> annotation, RegularParameter parameter) {
        super(element, annotation);
        this.parameter = parameter;
      }
      static final Function<TmpRegularParameter, RegularParameter> toValidParameter = new Function<TmpRegularParameter, RegularParameter>() {
        @Override
        public RegularParameter apply(TmpRegularParameter parameter) {
          return parameter.parameter;
        }
      };
      static TmpRegularParameter create(VariableElement element, Optional<String> projectionMethodName) {
        Optional<Step> annotation = fromNullable(element.getAnnotation(Step.class));
        boolean nonNull = annotation.isPresent() && annotation.get().nonNull();
        RegularParameter parameter = new RegularParameter(element.getSimpleName().toString(), TypeName.get(element.asType()), projectionMethodName, nonNull);
        return new TmpRegularParameter(element, annotation, parameter);
      }
    }

    static final class TmpAccessorPair extends TmpValidParameter {
      private final AccessorPair accessorPair;
      private TmpAccessorPair(Element element, Optional<Step> annotation, AccessorPair accessorPair) {
        super(element, annotation);
        this.accessorPair = accessorPair;
      }
      static final Function<TmpAccessorPair, AccessorPair> toValidParameter = new Function<TmpAccessorPair, AccessorPair>() {
        @Override
        public AccessorPair apply(TmpAccessorPair parameter) {
          return parameter.accessorPair;
        }
      };
      static TmpAccessorPair create(ExecutableElement getter, CollectionType collectionType) {
        Optional<Step> annotation = fromNullable(getter.getAnnotation(Step.class));
        boolean nonNull = annotation.isPresent() && annotation.get().nonNull();
        AccessorPair accessorPair = new AccessorPair(TypeName.get(getter.getReturnType()), getter.getSimpleName().toString(), collectionType, nonNull);
        return new TmpAccessorPair(getter, annotation, accessorPair);
      }
    }

    private TmpValidParameter(Element element, Optional<Step> annotation) {
      this.element = element;
      this.annotation = annotation;
    }

  }

  public abstract static class ValidParameter {

    public final String name;
    public final TypeName type;
    public final boolean nonNull;

    public static final class RegularParameter extends ValidParameter {
      public final Optional<String> projectionMethodName;
      RegularParameter(String name, TypeName type, Optional<String> projectionMethodName, boolean nonNull) {
        super(name, type, nonNull);
        this.projectionMethodName = projectionMethodName;
      }
    }

    public static final class AccessorPair extends ValidParameter {
      public final String projectionMethodName;

      /**
       * Only present if there is no setter for the collection.
       * If {@link #type} is {@code List<String>}, this would be {@code String}
       */
      public final CollectionType collectionType;

      AccessorPair(TypeName type, String projectionMethodName, CollectionType collectionType, boolean nonNull) {
        super(name(projectionMethodName), type, nonNull);
        this.projectionMethodName = projectionMethodName;
        this.collectionType = collectionType;
      }

      private static String name(String projectionMethodName) {
        return downcase(projectionMethodName.substring(projectionMethodName.startsWith("is") ? 2 : 3));
      }
    }

    ValidParameter(String name, TypeName type, boolean nonNull) {
      this.name = name;
      this.type = type;
      this.nonNull = nonNull;
    }
  }

  public static abstract class ValidationResult {
    public static abstract class ValidationResultCases<R> {
      abstract R executableGoal(Analyser.ExecutableGoal goal, ImmutableList<RegularParameter> parameters);
      abstract R beanGoal(BeanGoal beanGoal, ImmutableList<AccessorPair> accessorPairs);
    }
    abstract <R> R accept(ValidationResultCases<R> cases);
    static final class RegularValidationResult extends ValidationResult {
      private final Analyser.ExecutableGoal goal;
      private final ImmutableList<RegularParameter> parameters;
      RegularValidationResult(Analyser.ExecutableGoal goal, ImmutableList<RegularParameter> parameters) {
        this.goal = goal;
        this.parameters = parameters;
      }
      @Override
      <R> R accept(ValidationResultCases<R> cases) {
        return cases.executableGoal(goal, parameters);
      }
    }

    static final class BeanValidationResult extends ValidationResult {
      private final BeanGoal goal;
      private final ImmutableList<AccessorPair> accessorPairs;
      BeanValidationResult(BeanGoal goal, ImmutableList<AccessorPair> accessorPairs) {
        this.goal = goal;
        this.accessorPairs = accessorPairs;
      }
      @Override
      <R> R accept(ValidationResultCases<R> cases) {
        return cases.beanGoal(goal, accessorPairs);
      }
    }
  }

  public static final class CollectionType {
    public final Optional<? extends TypeName> type;
    public final boolean allowShortcut;

    private CollectionType(Optional<? extends TypeName> type, boolean allowShortcut) {
      this.type = type;
      this.allowShortcut = allowShortcut;
    }
    private static final CollectionType absent = new CollectionType(Optional.<TypeName>absent(), false);
    private static CollectionType of(TypeName type, boolean allowShortcut) {
      return new CollectionType(Optional.of(type), allowShortcut);
    }
  }

  private ProjectionValidator() {
    throw new UnsupportedOperationException("no instances");
  }
}
