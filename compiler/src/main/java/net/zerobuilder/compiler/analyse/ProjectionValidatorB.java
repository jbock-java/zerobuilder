package net.zerobuilder.compiler.analyse;

import com.palantir.javapoet.ClassName;
import com.palantir.javapoet.TypeName;
import java.util.Optional;
import javax.lang.model.element.Element;
import javax.lang.model.element.ElementVisitor;
import javax.lang.model.type.DeclaredType;
import javax.lang.model.type.TypeVisitor;
import javax.lang.model.util.SimpleElementVisitor9;
import javax.lang.model.util.SimpleTypeVisitor9;
import net.zerobuilder.IgnoreGetter;
import net.zerobuilder.compiler.analyse.DtoGoalElement.BeanGoalElement;
import net.zerobuilder.compiler.analyse.ProjectionValidator.TmpAccessorPair;
import net.zerobuilder.compiler.generate.DtoBeanGoalDescription.BeanGoalDescription;
import net.zerobuilder.compiler.generate.DtoBeanParameter;
import net.zerobuilder.compiler.generate.DtoBeanParameter.AbstractBeanParameter;

import javax.lang.model.element.ElementKind;
import javax.lang.model.element.ExecutableElement;
import javax.lang.model.element.NestingKind;
import javax.lang.model.element.TypeElement;
import javax.lang.model.type.TypeKind;
import javax.lang.model.type.TypeMirror;
import java.util.Collection;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import static com.palantir.javapoet.ClassName.OBJECT;
import static javax.lang.model.element.Modifier.ABSTRACT;
import static javax.lang.model.element.Modifier.PRIVATE;
import static javax.lang.model.element.Modifier.STATIC;
import static javax.lang.model.type.TypeKind.DECLARED;
import static javax.lang.model.util.ElementFilter.constructorsIn;
import static net.zerobuilder.compiler.Messages.ErrorMessages.BEAN_ABSTRACT_CLASS;
import static net.zerobuilder.compiler.Messages.ErrorMessages.BEAN_COULD_NOT_FIND_SETTER;
import static net.zerobuilder.compiler.Messages.ErrorMessages.BEAN_NO_DEFAULT_CONSTRUCTOR;
import static net.zerobuilder.compiler.Messages.ErrorMessages.NESTING_KIND;
import static net.zerobuilder.compiler.Messages.ErrorMessages.BEAN_NO_SUPERCLASS;
import static net.zerobuilder.compiler.Messages.ErrorMessages.BEAN_NO_INTERFACES;
import static net.zerobuilder.compiler.Messages.ErrorMessages.TYPE_PARAMS_BEAN;
import static net.zerobuilder.compiler.analyse.ProjectionValidator.TmpAccessorPair.accessorPair;
import static net.zerobuilder.compiler.analyse.ProjectionValidator.TmpAccessorPair.toValidParameter;
import static net.zerobuilder.compiler.analyse.ProjectionValidator.shuffledParameters;
import static net.zerobuilder.compiler.analyse.Utilities.ClassNames.COLLECTION;
import static net.zerobuilder.compiler.analyse.Utilities.sortedCopy;
import static net.zerobuilder.compiler.analyse.Utilities.thrownTypes;
import static net.zerobuilder.compiler.common.LessElements.getLocalAndInheritedMethods;
import static net.zerobuilder.compiler.common.LessTypes.asTypeElement;
import static net.zerobuilder.compiler.generate.ZeroUtil.transform;
import static net.zerobuilder.compiler.generate.ZeroUtil.upcase;

final class ProjectionValidatorB {

  private static final Comparator<TmpAccessorPair> ALPHABETIC_SORT
      = Comparator.comparing(a -> a.parameter.name());

  static final Predicate<String> IS_GETTER_NAME = Pattern.compile("^get[A-Z].*$|^is[A-Z].*$").asPredicate()
      .and(name -> !"getClass".equals(name));

  private static final Predicate<String> IS_SETTER_NAME = Pattern.compile("^set[A-Z].*$").asPredicate();

  private static final Predicate<ExecutableElement> LOOKS_LIKE_SETTER = method ->
      method.getKind() == ElementKind.METHOD
          && method.getParameters().size() == 1
          && !method.getModifiers().contains(PRIVATE)
          && !method.getModifiers().contains(STATIC)
          && !method.getModifiers().contains(ABSTRACT)
          && method.getReturnType().getKind() == TypeKind.VOID
          && IS_SETTER_NAME.test(method.getSimpleName().toString());

  private static final Predicate<ExecutableElement> LOOKS_LIKE_GETTER = method ->
      method.getKind() == ElementKind.METHOD
          && method.getParameters().isEmpty()
          && !method.getModifiers().contains(PRIVATE)
          && !method.getModifiers().contains(STATIC)
          && !method.getModifiers().contains(ABSTRACT)
          && !method.getReturnType().getKind().equals(TypeKind.VOID)
          && IS_GETTER_NAME.test(method.getSimpleName().toString())
          && method.getAnnotation(IgnoreGetter.class) == null;

  static final Function<BeanGoalElement, BeanGoalDescription> validateBean
      = goal -> {
    validateBeanType(goal.beanType());
    Collection<ExecutableElement> getters = getters(goal);
    Map<String, ExecutableElement> settersByName = setters(goal.beanType(), getters);
    List<TmpAccessorPair> builder = getters.stream()
        .map(getter -> tmpAccessorPair(settersByName, getter))
        .collect(Collectors.toList());
    return createResult(goal, builder);
  };

  private static TmpAccessorPair tmpAccessorPair(Map<String, ExecutableElement> settersByName, ExecutableElement getter) {
    ExecutableElement setter = settersByName.get(setterName(getter));
    return setter == null
        ? loneGetter(getter)
        : accessorPair(getter, setter);
  }

  private static TmpAccessorPair loneGetter(ExecutableElement getter) {
    TypeMirror type = getter.getReturnType();
    String name = getter.getSimpleName().toString();
    if (!isImplementationOf(type, COLLECTION)) {
      throw new ValidationException(BEAN_COULD_NOT_FIND_SETTER, getter);
    }
    TypeName typeName = TypeName.get(type);
    AbstractBeanParameter loneGetter = DtoBeanParameter.loneGetter(typeName, name, thrownTypes(getter));
    return TmpAccessorPair.createLoneGetter(getter, loneGetter);
  }

  private static Collection<ExecutableElement> getters(BeanGoalElement goal) {
    return getLocalAndInheritedMethods(goal.beanType(), LOOKS_LIKE_GETTER).values();
  }

  private static Map<String, ExecutableElement> setters(TypeElement beanType,
                                                        Collection<ExecutableElement> getters) {
    Predicate<ExecutableElement> filter = LOOKS_LIKE_SETTER
        .and(setterSieve(getters));
    return getLocalAndInheritedMethods(beanType, filter);
  }

  private abstract static class OptionalTypeVisitor<E> extends SimpleTypeVisitor9<Optional<E>, Void> {

    @Override
    protected final Optional<E> defaultAction(TypeMirror e, Void nothing) {
      return Optional.empty();
    }
  }

  public static final TypeVisitor<Optional<DeclaredType>, Void> AS_DECLARED = new OptionalTypeVisitor<>() {
    @Override
    public Optional<DeclaredType> visitDeclared(DeclaredType declaredType, Void nothing) {
      return Optional.of(declaredType);
    }
  };

  public static final ElementVisitor<Optional<TypeElement>, Void> AS_TYPE_ELEMENT = new SimpleElementVisitor9<>() {
    @Override
    public Optional<TypeElement> visitType(TypeElement typeElement, Void nothing) {
      return Optional.of(typeElement);
    }

    @Override
    protected Optional<TypeElement> defaultAction(Element e, Void nothing) {
      return Optional.empty();
    }
  };

  static void validateNoInterfaces(TypeElement beanType) {
    AS_DECLARED.visit(beanType.getSuperclass())
        .map(DeclaredType::asElement)
        .flatMap(AS_TYPE_ELEMENT::visit)
        .flatMap(superClass -> superClass.getSuperclass().getKind() == TypeKind.NONE ?
            Optional.empty() :
            Optional.of(BEAN_NO_SUPERCLASS))
        .ifPresent(error -> {
          throw new ValidationException(error, beanType);
        });
    if (!beanType.getInterfaces().isEmpty()) {
      throw new ValidationException(BEAN_NO_INTERFACES, beanType);
    }
  }

  private static void validateBeanType(TypeElement beanType) {
    validateNoInterfaces(beanType);
    if (!hasParameterlessConstructor(beanType)) {
      throw new ValidationException(BEAN_NO_DEFAULT_CONSTRUCTOR, beanType);
    }
    if (beanType.getModifiers().contains(PRIVATE)) {
      throw new ValidationException(NESTING_KIND, beanType);
    }
    if (beanType.getNestingKind() == NestingKind.MEMBER &&
        !beanType.getModifiers().contains(STATIC)) {
      throw new ValidationException(NESTING_KIND, beanType);
    }
    if (beanType.getModifiers().contains(ABSTRACT)) {
      throw new ValidationException(BEAN_ABSTRACT_CLASS, beanType);
    }
    if (!beanType.getTypeParameters().isEmpty()) {
      throw new ValidationException(TYPE_PARAMS_BEAN, beanType);
    }
  }

  private record SetterTest(String name, TypeName type) implements Predicate<ExecutableElement> {

    private static SetterTest fromGetter(ExecutableElement getter) {
      String name = getter.getSimpleName().toString();
      String setterName = "set" + upcase(name.substring(name.startsWith("get") ? 3 : 2));
      return new SetterTest(setterName, TypeName.get(getter.getReturnType()));
    }

    @Override
    public boolean test(ExecutableElement setter) {
      return setter.getParameters().size() == 1
          && name.equals(setter.getSimpleName().toString())
          && type.equals(TypeName.get(setter.getParameters().getFirst().asType()));
    }
  }

  /**
   * Deal with overloaded setters
   *
   * @param getters getters
   * @return predicate
   */
  private static Predicate<ExecutableElement> setterSieve(Collection<ExecutableElement> getters) {
    List<SetterTest> setterTests = getters.stream()
        .map(SetterTest::fromGetter)
        .toList();
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
    return "set" + name.substring(name.startsWith("get") ? 3 : 2);
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

  private static BeanGoalDescription createResult(BeanGoalElement goal, List<TmpAccessorPair> tmpAccessorPairs) {
    List<TmpAccessorPair> sorted = sortedCopy(tmpAccessorPairs, ALPHABETIC_SORT);
    List<AbstractBeanParameter> validBeanParameters
        = transform(shuffledParameters(sorted), toValidParameter);
    return BeanGoalDescription.create(goal.details(), validBeanParameters,
        beanConstructorExceptions(goal));
  }

  private static List<TypeName> beanConstructorExceptions(BeanGoalElement goal) {
    for (ExecutableElement constructor : constructorsIn(goal.beanType().getEnclosedElements())) {
      if (constructor.getParameters().isEmpty()) {
        return thrownTypes(constructor);
      }
    }
    throw new IllegalStateException("should never happen");
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
