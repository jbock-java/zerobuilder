package net.zerobuilder.compiler.analyse;

import com.google.common.base.Function;
import com.google.common.base.Predicate;
import com.google.common.collect.FluentIterable;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Ordering;
import com.squareup.javapoet.ClassName;
import com.squareup.javapoet.TypeName;
import net.zerobuilder.Goal;
import net.zerobuilder.Ignore;
import net.zerobuilder.Step;
import net.zerobuilder.compiler.analyse.DtoPackage.GoalTypes.BeanGoalElement;
import net.zerobuilder.compiler.analyse.DtoShared.ValidBeanParameter;
import net.zerobuilder.compiler.analyse.DtoShared.ValidBeanParameter.CollectionType;
import net.zerobuilder.compiler.analyse.DtoValidGoal.ValidBeanGoal;
import net.zerobuilder.compiler.analyse.DtoValidGoal.ValidGoal;
import net.zerobuilder.compiler.analyse.ProjectionValidator.TmpValidParameter.TmpAccessorPair;

import javax.lang.model.element.ElementKind;
import javax.lang.model.element.ExecutableElement;
import javax.lang.model.element.TypeElement;
import javax.lang.model.type.TypeKind;
import javax.lang.model.type.TypeMirror;
import java.util.Collection;
import java.util.Comparator;
import java.util.List;

import static com.google.auto.common.MoreElements.getLocalAndInheritedMethods;
import static com.google.auto.common.MoreTypes.asDeclared;
import static com.google.auto.common.MoreTypes.asTypeElement;
import static com.google.common.base.Ascii.isUpperCase;
import static com.google.common.collect.Iterables.getOnlyElement;
import static javax.lang.model.element.Modifier.PUBLIC;
import static javax.lang.model.element.Modifier.STATIC;
import static javax.lang.model.util.ElementFilter.constructorsIn;
import static net.zerobuilder.compiler.Messages.ErrorMessages.BAD_GENERICS;
import static net.zerobuilder.compiler.Messages.ErrorMessages.COULD_NOT_FIND_SETTER;
import static net.zerobuilder.compiler.Messages.ErrorMessages.GETTER_EXCEPTION;
import static net.zerobuilder.compiler.Messages.ErrorMessages.GETTER_SETTER_TYPE_MISMATCH;
import static net.zerobuilder.compiler.Messages.ErrorMessages.IGNORE_AND_STEP;
import static net.zerobuilder.compiler.Messages.ErrorMessages.IGNORE_ON_SETTER;
import static net.zerobuilder.compiler.Messages.ErrorMessages.NO_ACCESSOR_PAIRS;
import static net.zerobuilder.compiler.Messages.ErrorMessages.NO_DEFAULT_CONSTRUCTOR;
import static net.zerobuilder.compiler.Messages.ErrorMessages.SETTER_EXCEPTION;
import static net.zerobuilder.compiler.Messages.ErrorMessages.STEP_ON_SETTER;
import static net.zerobuilder.compiler.Messages.ErrorMessages.TARGET_PUBLIC;
import static net.zerobuilder.compiler.analyse.ProjectionValidator.TmpValidParameter.TmpAccessorPair.toValidParameter;
import static net.zerobuilder.compiler.analyse.ProjectionValidator.shuffledParameters;

public final class ProjectionValidatorB {

  private static final Ordering<TmpAccessorPair> ACCESSOR_PAIR_ORDERING
      = Ordering.from(new Comparator<TmpAccessorPair>() {
    @Override
    public int compare(TmpAccessorPair pair0, TmpAccessorPair pair1) {
      return pair0.validBeanParameter.name.compareTo(pair1.validBeanParameter.name);
    }
  });

  private static final ClassName OBJECT = ClassName.get(Object.class);
  private static final ClassName COLLECTION = ClassName.get(Collection.class);
  public static final ClassName ITERABLE = ClassName.get(Iterable.class);

  static final Function<BeanGoalElement, ValidGoal> validateBean
      = new Function<BeanGoalElement, ValidGoal>() {
    @Override
    public ValidGoal apply(BeanGoalElement goal) {
      ImmutableMap<String, ExecutableElement> setters = setters(goal);
      ImmutableList.Builder<TmpAccessorPair> builder = ImmutableList.builder();
      for (ExecutableElement getter : getters(goal)) {
        ExecutableElement setter = setters.get(setterName(getter));
        builder.add(setter == null
            ? setterlessAccessorPair(getter, goal.goalAnnotation)
            : regularAccessorPair(getter, setter, goal.goalAnnotation));
      }
      ImmutableList<TmpAccessorPair> tmpAccessorPairs = builder.build();
      if (tmpAccessorPairs.isEmpty()) {
        throw new ValidationException(NO_ACCESSOR_PAIRS, goal.beanTypeElement);
      }
      return createResult(goal, tmpAccessorPairs);
    }
  };

  private static TmpAccessorPair setterlessAccessorPair(ExecutableElement getter, Goal goalAnnotation) {
    if (!isImplementationOf(getter.getReturnType(), COLLECTION)) {
      throw new ValidationException(COULD_NOT_FIND_SETTER, getter);
    }
    // no setter but we have a getter that returns something like List<E>
    // in this case we need to find what E is ("collectionType")
    List<? extends TypeMirror> typeArguments = asDeclared(getter.getReturnType()).getTypeArguments();
    if (typeArguments.isEmpty()) {
      // raw collection
      return TmpAccessorPair.create(getter, CollectionType.of(Object.class, false), goalAnnotation);
    } else if (typeArguments.size() == 1) {
      // one type parameter
      TypeMirror collectionType = getOnlyElement(typeArguments);
      boolean allowShortcut = !ClassName.get(asTypeElement(collectionType)).equals(ITERABLE);
      return TmpAccessorPair.create(getter, CollectionType.of(collectionType, allowShortcut), goalAnnotation);
    } else {
      // unlikely: subclass of Collection should not have more than one type parameter
      throw new ValidationException(BAD_GENERICS, getter);
    }
  }

  private static TmpAccessorPair regularAccessorPair(ExecutableElement getter, ExecutableElement setter, Goal goalAnnotation) {
    TypeName setterType = TypeName.get(setter.getParameters().get(0).asType());
    TypeName getterType = TypeName.get(getter.getReturnType());
    if (!setterType.equals(getterType)) {
      throw new ValidationException(GETTER_SETTER_TYPE_MISMATCH, setter);
    }
    if (!getter.getThrownTypes().isEmpty()) {
      throw new ValidationException(GETTER_EXCEPTION, getter);
    }
    return TmpAccessorPair.createRegular(getter, goalAnnotation);
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
    if (className.equals(OBJECT)) {
      return false;
    }
    for (TypeMirror anInterface : element.getInterfaces()) {
      if (isImplementationOf(anInterface, test)) {
        return true;
      }
    }
    return false;
  }

  private static ImmutableList<ExecutableElement> getters(BeanGoalElement goal) {
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
        .filter(new Predicate<ExecutableElement>() {
          @Override
          public boolean apply(ExecutableElement getter) {
            Ignore ignoreAnnotation = getter.getAnnotation(Ignore.class);
            if (ignoreAnnotation != null && getter.getAnnotation(Step.class) != null) {
              throw new ValidationException(IGNORE_AND_STEP, getter);
            }
            return ignoreAnnotation == null;
          }
        })
        .toList();
  }

  private static ImmutableMap<String, ExecutableElement> setters(BeanGoalElement goal) throws ValidationException {
    TypeElement beanType = goal.beanTypeElement;
    if (!hasParameterlessConstructor(beanType)) {
      throw new ValidationException(NO_DEFAULT_CONSTRUCTOR, beanType);
    }
    if (!beanType.getModifiers().contains(PUBLIC)) {
      throw new ValidationException(TARGET_PUBLIC, beanType);
    }
    return FluentIterable.from(getLocalAndInheritedMethods(beanType, goal.elements))
        .filter(new Predicate<ExecutableElement>() {
          @Override
          public boolean apply(ExecutableElement method) {
            return method.getKind() == ElementKind.METHOD
                && method.getModifiers().contains(PUBLIC)
                && method.getSimpleName().length() >= 4
                && isUpperCase(method.getSimpleName().charAt(3))
                && method.getSimpleName().toString().startsWith("set")
                && method.getParameters().size() == 1
                && method.getReturnType().getKind() == TypeKind.VOID;
          }
        })
        .filter(new Predicate<ExecutableElement>() {
          @Override
          public boolean apply(ExecutableElement setter) {
            if (setter.getThrownTypes().isEmpty()) {
              return true;
            } else {
              throw new ValidationException(SETTER_EXCEPTION, setter);
            }
          }
        })
        .filter(new Predicate<ExecutableElement>() {
          @Override
          public boolean apply(ExecutableElement setter) {
            if (setter.getAnnotation(Step.class) != null) {
              throw new ValidationException(STEP_ON_SETTER, setter);
            }
            if (setter.getAnnotation(Ignore.class) != null) {
              throw new ValidationException(IGNORE_ON_SETTER, setter);
            }
            return true;
          }
        })
        .uniqueIndex(new Function<ExecutableElement, String>() {
          @Override
          public String apply(ExecutableElement setter) {
            return setter.getSimpleName().toString().substring(3);
          }
        });
  }

  private static String setterName(ExecutableElement getter) {
    String name = getter.getSimpleName().toString();
    return name.substring(name.startsWith("get") ? 3 : 2);
  }

  private static boolean hasParameterlessConstructor(TypeElement type) {
    for (ExecutableElement constructor : constructorsIn(type.getEnclosedElements())) {
      if (constructor.getParameters().isEmpty()
          && constructor.getModifiers().contains(PUBLIC)) {
        return true;
      }
    }
    return false;
  }

  private static ValidGoal createResult(BeanGoalElement goal, ImmutableList<TmpAccessorPair> tmpAccessorPairs) {
    ImmutableList<ValidBeanParameter> validBeanParameters
        = FluentIterable.from(shuffledParameters(ACCESSOR_PAIR_ORDERING.immutableSortedCopy(tmpAccessorPairs)))
        .transform(toValidParameter)
        .toList();
    return new ValidBeanGoal(goal, validBeanParameters);
  }

  private ProjectionValidatorB() {
    throw new UnsupportedOperationException("no instances");
  }
}
