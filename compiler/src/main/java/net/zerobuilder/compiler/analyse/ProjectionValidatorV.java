package net.zerobuilder.compiler.analyse;

import com.squareup.javapoet.TypeName;
import net.zerobuilder.compiler.analyse.DtoGoalElement.RegularGoalElement;
import net.zerobuilder.compiler.analyse.ProjectionValidator.TmpRegularParameter;
import net.zerobuilder.compiler.generate.DtoGoalDescription.GoalDescription;
import net.zerobuilder.compiler.generate.DtoGoalDescription.RegularGoalDescription;
import net.zerobuilder.compiler.generate.DtoParameter;

import javax.lang.model.element.ExecutableElement;
import javax.lang.model.element.TypeElement;
import javax.lang.model.element.VariableElement;
import javax.lang.model.type.TypeKind;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.stream.Collectors;

import static javax.lang.model.element.Modifier.PRIVATE;
import static javax.lang.model.element.Modifier.STATIC;
import static javax.lang.model.util.ElementFilter.fieldsIn;
import static net.zerobuilder.compiler.Messages.ErrorMessages.NO_PROJECTION;
import static net.zerobuilder.compiler.Messages.ErrorMessages.PROJECTION_EXCEPTIONS;
import static net.zerobuilder.compiler.analyse.ProjectionValidator.TmpRegularParameter.toValidParameter;
import static net.zerobuilder.compiler.analyse.ProjectionValidator.shuffledParameters;
import static net.zerobuilder.compiler.analyse.Utilities.transform;
import static net.zerobuilder.compiler.analyse.Utilities.upcase;
import static net.zerobuilder.compiler.common.LessElements.getLocalAndInheritedMethods;
import static net.zerobuilder.compiler.common.LessTypes.asTypeElement;

final class ProjectionValidatorV {

  private static final java.util.function.Predicate<ExecutableElement> LOOKS_LIKE_PROJECTION = method -> method.getParameters().isEmpty()
      && !method.getModifiers().contains(PRIVATE)
      && method.getReturnType().getKind() != TypeKind.VOID
      && !method.getModifiers().contains(STATIC)
      && !"getClass".equals(method.getSimpleName().toString())
      && !"clone".equals(method.getSimpleName().toString());

  private static final Predicate<ExecutableElement> DECLARES_NO_EXCEPTIONS = method -> {
    if (method.getThrownTypes().isEmpty()) {
      return true;
    }
    // projections may currently not declare exceptions
    throw new ValidationException(PROJECTION_EXCEPTIONS + ": " + method.getSimpleName(), method);
  };

  static final Function<RegularGoalElement, GoalDescription> validateValue
      = goal -> {
    TypeElement type = asTypeElement(goal.executableElement.getEnclosingElement().asType());
    Set<String> methodNames = methodNames(type);
    Map<String, List<VariableElement>> fieldsByName = fields(type);
    List<TmpRegularParameter> builder = new ArrayList<>();
    for (VariableElement parameter : goal.executableElement.getParameters()) {
      List<VariableElement> fields = fieldsByName.get(parameter.getSimpleName().toString());
      if (fields != null && fields.size() != 1) {
        throw new IllegalStateException("field name should be unique");
      }
      if (fields != null && TypeName.get(fields.get(0).asType()).equals(TypeName.get(parameter.asType()))) {
        builder.add(TmpRegularParameter.create(parameter, Optional.empty(), goal.goalAnnotation));
      } else {
        String methodName = "get" + upcase(parameter.getSimpleName().toString());
        if (!methodNames.contains(methodName)
            && TypeName.get(parameter.asType()) == TypeName.BOOLEAN) {
          methodName = "is" + upcase(parameter.getSimpleName().toString());
        }
        if (!methodNames.contains(methodName)) {
          methodName = parameter.getSimpleName().toString();
        }
        if (!methodNames.contains(methodName)) {
          throw new ValidationException(NO_PROJECTION, parameter);
        }
        builder.add(TmpRegularParameter.create(parameter, Optional.of(methodName), goal.goalAnnotation));
      }
    }
    return createGoalDescription(goal, builder);
  };

  private static Map<String, List<VariableElement>> fields(TypeElement type) {
    List<VariableElement> variableElements = fieldsIn(type.getEnclosedElements());
    return variableElements.stream()
        .filter(field -> !field.getModifiers().contains(PRIVATE)
            && !field.getModifiers().contains(STATIC))
        .collect(Collectors.groupingBy(field -> field.getSimpleName().toString()));
  }

  private static Set<String> methodNames(TypeElement type) {
    Predicate<ExecutableElement> filter = LOOKS_LIKE_PROJECTION;
    return getLocalAndInheritedMethods(type, filter).stream().filter(DECLARES_NO_EXCEPTIONS)
        .map(method -> method.getSimpleName().toString())
        .collect(Collectors.toSet());
  }

  static final Function<RegularGoalElement, GoalDescription> validateValueIgnoreProjections
      = goal -> {
    ArrayList<TmpRegularParameter> builder = new ArrayList<>();
    for (VariableElement parameter : goal.executableElement.getParameters()) {
      builder.add(TmpRegularParameter.create(parameter, Optional.empty(), goal.goalAnnotation));
    }
    return createGoalDescription(goal, builder);
  };

  private static GoalDescription createGoalDescription(RegularGoalElement goal, List<TmpRegularParameter> parameters) {
    List<TmpRegularParameter> shuffled = shuffledParameters(parameters);
    return create(goal, transform(shuffled, toValidParameter));
  }

  private static RegularGoalDescription create(RegularGoalElement goal, List<DtoParameter.RegularParameter> parameters) {
    List<TypeName> thrownTypes = thrownTypes(goal.executableElement);
    return RegularGoalDescription.create(goal.details, thrownTypes, parameters);
  }

  private static List<TypeName> thrownTypes(ExecutableElement executableElement) {
    return transform(executableElement.getThrownTypes(), TypeName::get);
  }

  private ProjectionValidatorV() {
    throw new UnsupportedOperationException("no instances");
  }
}
