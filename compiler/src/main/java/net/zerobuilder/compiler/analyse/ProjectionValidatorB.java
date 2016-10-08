package net.zerobuilder.compiler.analyse;

import com.squareup.javapoet.ClassName;
import com.squareup.javapoet.TypeName;
import net.zerobuilder.Goal;
import net.zerobuilder.Ignore;
import net.zerobuilder.Step;
import net.zerobuilder.compiler.analyse.DtoGoalElement.BeanGoalElement;
import net.zerobuilder.compiler.analyse.ProjectionValidator.TmpAccessorPair;
import net.zerobuilder.compiler.generate.DtoBeanParameter.AbstractBeanParameter;
import net.zerobuilder.compiler.generate.DtoBeanParameter.LoneGetter;
import net.zerobuilder.compiler.generate.DtoGoalDescription.BeanGoalDescription;
import net.zerobuilder.compiler.generate.DtoGoalDescription.GoalDescription;

import javax.lang.model.element.ElementKind;
import javax.lang.model.element.ExecutableElement;
import javax.lang.model.element.TypeElement;
import javax.lang.model.type.TypeKind;
import javax.lang.model.type.TypeMirror;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import static com.squareup.javapoet.ClassName.OBJECT;
import static java.lang.Character.isUpperCase;
import static javax.lang.model.element.Modifier.PRIVATE;
import static javax.lang.model.element.Modifier.STATIC;
import static javax.lang.model.type.TypeKind.DECLARED;
import static javax.lang.model.util.ElementFilter.constructorsIn;
import static net.zerobuilder.compiler.Messages.ErrorMessages.BEAN_ACCESSOR_EXCEPTIONS;
import static net.zerobuilder.compiler.Messages.ErrorMessages.BEAN_COULD_NOT_FIND_SETTER;
import static net.zerobuilder.compiler.Messages.ErrorMessages.BEAN_GETTER_EXCEPTION;
import static net.zerobuilder.compiler.Messages.ErrorMessages.BEAN_GETTER_SETTER_TYPE_MISMATCH;
import static net.zerobuilder.compiler.Messages.ErrorMessages.BEAN_IGNORE_AND_STEP;
import static net.zerobuilder.compiler.Messages.ErrorMessages.BEAN_NO_ACCESSOR_PAIRS;
import static net.zerobuilder.compiler.Messages.ErrorMessages.BEAN_NO_DEFAULT_CONSTRUCTOR;
import static net.zerobuilder.compiler.Messages.ErrorMessages.BEAN_PRIVATE_CLASS;
import static net.zerobuilder.compiler.Messages.ErrorMessages.IGNORE_ON_SETTER;
import static net.zerobuilder.compiler.Messages.ErrorMessages.STEP_ON_SETTER;
import static net.zerobuilder.compiler.analyse.ProjectionValidator.TmpAccessorPair.toValidParameter;
import static net.zerobuilder.compiler.analyse.ProjectionValidator.TmpValidParameter.nonNull;
import static net.zerobuilder.compiler.analyse.ProjectionValidator.shuffledParameters;
import static net.zerobuilder.compiler.analyse.Utilities.ClassNames.COLLECTION;
import static net.zerobuilder.compiler.analyse.Utilities.sortedCopy;
import static net.zerobuilder.compiler.analyse.Utilities.transform;
import static net.zerobuilder.compiler.analyse.Utilities.upcase;
import static net.zerobuilder.compiler.common.LessElements.getLocalAndInheritedMethods;
import static net.zerobuilder.compiler.common.LessTypes.asTypeElement;
import static net.zerobuilder.compiler.generate.DtoBeanParameter.beanParameterName;

final class ProjectionValidatorB {

  private static final Comparator<TmpAccessorPair> ALPHABETIC_SORT
      = (pair0, pair1) -> {
    String name0 = pair0.validBeanParameter.accept(beanParameterName);
    String name1 = pair1.validBeanParameter.accept(beanParameterName);
    return name0.compareTo(name1);
  };

  static final Predicate<String> IS_GETTER_NAME = Pattern.compile("^get[A-Z].*$|^is[A-Z].*$").asPredicate()
      .and(name -> !"getClass".equals(name));

  private static final Predicate<String> IS_SETTER_NAME = Pattern.compile("^set[A-Z].*$").asPredicate();

  private static final Predicate<ExecutableElement> LOOKS_LIKE_SETTER = method ->
      method.getKind() == ElementKind.METHOD
          && method.getParameters().size() == 1
          && !method.getModifiers().contains(PRIVATE)
          && !method.getModifiers().contains(STATIC)
          && method.getReturnType().getKind() == TypeKind.VOID
          && IS_SETTER_NAME.test(method.getSimpleName().toString());


  private static final Predicate<ExecutableElement> LOOKS_LIKE_GETTER = method ->
      method.getKind() == ElementKind.METHOD
          && method.getParameters().isEmpty()
          && !method.getModifiers().contains(PRIVATE)
          && !method.getModifiers().contains(STATIC)
          && !method.getReturnType().getKind().equals(TypeKind.VOID)
          && IS_GETTER_NAME.test(method.getSimpleName().toString());

  static final Function<BeanGoalElement, GoalDescription> validateBean
      = goal -> {
    List<ExecutableElement> getters = getters(goal);
    Map<String, List<ExecutableElement>> settersByName = setters(goal, getters);
    List<TmpAccessorPair> builder = new ArrayList<>();
    for (ExecutableElement getter : getters) {
      List<ExecutableElement> setters = settersByName.get(setterName(getter));
      if (setters != null && setters.size() != 1) {
        throw new IllegalStateException("setter name should be unique");
      }
      builder.add(setters == null
          ? loneGetter(getter, goal.goalAnnotation)
          : regularAccessorPair(getter, setters.get(0), goal.goalAnnotation));
    }
    if (builder.isEmpty()) {
      throw new ValidationException(BEAN_NO_ACCESSOR_PAIRS, goal.beanType);
    }
    return createResult(goal, builder);
  };

  private static final Predicate<ExecutableElement> IS_NOT_IGNORED = getter -> {
    Ignore ignoreAnnotation = getter.getAnnotation(Ignore.class);
    if (ignoreAnnotation != null && getter.getAnnotation(Step.class) != null) {
      throw new ValidationException(BEAN_IGNORE_AND_STEP, getter);
    }
    return ignoreAnnotation == null;
  };

  private static final Predicate<ExecutableElement> DECLARES_NO_EXCEPTIONS = method -> {
    if (method.getThrownTypes().isEmpty()) {
      return true;
    }
    // accessors may currently not declare exceptions
    throw new ValidationException(BEAN_ACCESSOR_EXCEPTIONS, method);
  };

  private static final Predicate<ExecutableElement> DOES_NOT_HAVE_STEP_OR_IGNORE_ANNOTATIONS = setter -> {
    if (setter.getAnnotation(Step.class) != null) {
      throw new ValidationException(STEP_ON_SETTER, setter);
    }
    if (setter.getAnnotation(Ignore.class) != null) {
      throw new ValidationException(IGNORE_ON_SETTER, setter);
    }
    return true;
  };

  private static TmpAccessorPair loneGetter(ExecutableElement getter, Goal goalAnnotation) {
    TypeMirror type = getter.getReturnType();
    String name = getter.getSimpleName().toString();
    if (!isImplementationOf(type, COLLECTION)) {
      throw new ValidationException(BEAN_COULD_NOT_FIND_SETTER, getter);
    }
    // no setter but we have a getter that returns something like List<E>
    // in this case we need to find what E is ("collectionType")
    TypeName typeName = TypeName.get(type);
    boolean nonNull = nonNull(type, getter.getAnnotation(Step.class), goalAnnotation);
    LoneGetter loneGetter = LoneGetter.create(typeName, name, nonNull);
    return TmpAccessorPair.createLoneGetter(getter, loneGetter);
  }

  private static TmpAccessorPair regularAccessorPair(ExecutableElement getter, ExecutableElement setter, Goal goalAnnotation) {
    TypeName setterType = TypeName.get(setter.getParameters().get(0).asType());
    TypeName getterType = TypeName.get(getter.getReturnType());
    if (!setterType.equals(getterType)) {
      throw new ValidationException(BEAN_GETTER_SETTER_TYPE_MISMATCH, setter);
    }
    if (!getter.getThrownTypes().isEmpty()) {
      throw new ValidationException(BEAN_GETTER_EXCEPTION, getter);
    }
    return TmpAccessorPair.createAccessorPair(getter, goalAnnotation);
  }

  private static List<ExecutableElement> getters(BeanGoalElement goal) {
    Predicate<ExecutableElement> filter = LOOKS_LIKE_GETTER
        .and(IS_NOT_IGNORED)
        .and(DECLARES_NO_EXCEPTIONS);
    return getLocalAndInheritedMethods(goal.beanType, filter)
        .stream()
        .collect(Collectors.toList());
  }

  private static Map<String, List<ExecutableElement>> setters(BeanGoalElement goal,
                                                              List<ExecutableElement> getters) {
    TypeElement beanType = goal.beanType;
    if (!hasParameterlessConstructor(beanType)) {
      throw new ValidationException(BEAN_NO_DEFAULT_CONSTRUCTOR, beanType);
    }
    if (beanType.getModifiers().contains(PRIVATE)) {
      throw new ValidationException(BEAN_PRIVATE_CLASS, beanType);
    }
    Predicate<ExecutableElement> filter = LOOKS_LIKE_SETTER
        .and(setterSieve(getters))
        .and(DECLARES_NO_EXCEPTIONS)
        .and(DOES_NOT_HAVE_STEP_OR_IGNORE_ANNOTATIONS);
    return getLocalAndInheritedMethods(beanType, filter).stream()
        .collect(Collectors.groupingBy(setter -> setter.getSimpleName().toString().substring(3)));
  }

  static final class SetterTest implements Predicate<ExecutableElement> {

    private final String name;
    private final TypeName type;

    private SetterTest(String name, TypeName type) {
      this.name = name;
      this.type = type;
    }

    private static SetterTest fromGetter(ExecutableElement getter) {
      String name = getter.getSimpleName().toString();
      String setterName = "set" + upcase(name.substring(name.startsWith("get") ? 3 : 2));
      return new SetterTest(setterName, TypeName.get(getter.getReturnType()));
    }

    @Override
    public boolean test(ExecutableElement setter) {
      return setter.getParameters().size() == 1
          && name.equals(setter.getSimpleName().toString())
          && type.equals(TypeName.get(setter.getParameters().get(0).asType()));
    }
  }

  /**
   * Deal with overloaded setters
   *
   * @param getters getters
   * @return predicate
   */
  private static Predicate<ExecutableElement> setterSieve(List<ExecutableElement> getters) {
    List<SetterTest> setterTests = getters.stream()
        .map(SetterTest::fromGetter)
        .collect(Collectors.toList());
    return setter -> {
      for (SetterTest test : setterTests) {
        if (test.test(setter)) {
          return true;
        }
      }
      return false;
    };
  }

  private static String setterName(ExecutableElement getter) {
    String name = getter.getSimpleName().toString();
    return name.substring(name.startsWith("get") ? 3 : 2);
  }

  private static boolean hasParameterlessConstructor(TypeElement type) {
    for (ExecutableElement constructor : constructorsIn(type.getEnclosedElements())) {
      if (constructor.getParameters().isEmpty()
          && !constructor.getModifiers().contains(PRIVATE)) {
        return true;
      }
    }
    return false;
  }

  private static GoalDescription createResult(BeanGoalElement goal, List<TmpAccessorPair> tmpAccessorPairs) {
    List<TmpAccessorPair> sorted = sortedCopy(tmpAccessorPairs, ALPHABETIC_SORT);
    List<AbstractBeanParameter> validBeanParameters
        = transform(shuffledParameters(sorted), toValidParameter);
    return BeanGoalDescription.create(goal.details, validBeanParameters);
  }

  private static boolean isImplementationOf(TypeMirror typeMirror, ClassName test) {
    if (typeMirror.getKind() != DECLARED) {
      return false;
    }
    TypeElement element = asTypeElement(typeMirror);
    return implementationOf(element, test);
  }

  private static boolean implementationOf(TypeElement element, ClassName test) {
    ClassName className = ClassName.get(element);
    if (className.equals(test)) {
      return true;
    }
    if (className.equals(OBJECT)) {
      return false;
    }
    for (TypeMirror superInterface : element.getInterfaces()) {
      if (implementationOf(asTypeElement(superInterface), test)) {
        return true;
      }
    }
    return false;
  }

  private ProjectionValidatorB() {
    throw new UnsupportedOperationException("no instances");
  }
}
