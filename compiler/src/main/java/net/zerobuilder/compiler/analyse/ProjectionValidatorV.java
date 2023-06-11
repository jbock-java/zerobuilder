package net.zerobuilder.compiler.analyse;

import io.jbock.javapoet.TypeName;
import net.zerobuilder.compiler.analyse.DtoGoalElement.RegularGoalElement;
import net.zerobuilder.compiler.analyse.DtoGoalElement.RegularProjectableGoalElement;
import net.zerobuilder.compiler.analyse.ProjectionValidator.TmpProjectedParameter;
import net.zerobuilder.compiler.analyse.ProjectionValidator.TmpSimpleParameter;
import net.zerobuilder.compiler.generate.DtoProjectionInfo.FieldAccess;
import net.zerobuilder.compiler.generate.DtoProjectionInfo.ProjectionInfo;
import net.zerobuilder.compiler.generate.DtoProjectionInfo.ProjectionMethod;
import net.zerobuilder.compiler.generate.DtoRegularGoalDescription.ProjectedRegularGoalDescription;
import net.zerobuilder.compiler.generate.DtoRegularGoalDescription.SimpleRegularGoalDescription;

import javax.lang.model.element.ExecutableElement;
import javax.lang.model.element.TypeElement;
import javax.lang.model.element.VariableElement;
import javax.lang.model.type.TypeKind;
import javax.lang.model.type.TypeMirror;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.function.Predicate;

import static java.util.Collections.emptyList;
import static javax.lang.model.element.ElementKind.CONSTRUCTOR;
import static javax.lang.model.element.Modifier.ABSTRACT;
import static javax.lang.model.element.Modifier.PRIVATE;
import static javax.lang.model.element.Modifier.STATIC;
import static net.zerobuilder.compiler.Messages.ErrorMessages.ABSTRACT_CONSTRUCTOR;
import static net.zerobuilder.compiler.Messages.ErrorMessages.MISSING_PROJECTION;
import static net.zerobuilder.compiler.analyse.DtoGoalElement.executableElement;
import static net.zerobuilder.compiler.analyse.ProjectionValidator.TmpProjectedParameter.toValidParameter;
import static net.zerobuilder.compiler.analyse.ProjectionValidator.shuffledParameters;
import static net.zerobuilder.compiler.analyse.Utilities.thrownTypes;
import static net.zerobuilder.compiler.common.LessElements.getLocalAndInheritedFields;
import static net.zerobuilder.compiler.common.LessElements.getLocalAndInheritedMethods;
import static net.zerobuilder.compiler.common.LessTypes.asTypeElement;
import static net.zerobuilder.compiler.common.LessTypes.isDeclaredType;
import static net.zerobuilder.compiler.generate.ZeroUtil.transform;
import static net.zerobuilder.compiler.generate.ZeroUtil.upcase;

final class ProjectionValidatorV {

  private static final Predicate<ExecutableElement> LOOKS_LIKE_PROJECTION =
      method -> method.getParameters().isEmpty()
          && !method.getModifiers().contains(PRIVATE)
          && !method.getModifiers().contains(STATIC)
          && method.getReturnType().getKind() != TypeKind.VOID
          && !"getClass".equals(method.getSimpleName().toString())
          && !"clone".equals(method.getSimpleName().toString());

  static final Function<RegularProjectableGoalElement, ProjectedRegularGoalDescription> validateUpdater =
      goal -> {
        TypeMirror mirror = goal.executableElement.getKind() == CONSTRUCTOR ?
            goal.executableElement.getEnclosingElement().asType() :
            goal.executableElement.getReturnType();
        if (!isDeclaredType(mirror)) {
          return createGoalDescription(goal, emptyList());
        }
        TypeElement type = asTypeElement(mirror);
        validateType(goal, type);
        Map<String, ExecutableElement> methods = getLocalAndInheritedMethods(type, LOOKS_LIKE_PROJECTION);
        Map<String, VariableElement> fields = getLocalAndInheritedFields(type);
        List<TmpProjectedParameter> parameters = transform(goal.executableElement.getParameters(),
            parameter -> TmpProjectedParameter.create(parameter,
                projectionInfo(methods, fields, parameter)));
        return createGoalDescription(goal, parameters);
      };

  private static ProjectionInfo projectionInfo(Map<String, ExecutableElement> methods,
                                               Map<String, VariableElement> fields,
                                               VariableElement parameter) {
    String name = parameter.getSimpleName().toString();
    VariableElement field = fields.get(name);
    TypeName parameterType = TypeName.get(parameter.asType());
    if (field != null && TypeName.get(field.asType()).equals(parameterType)) {
      return FieldAccess.create(field.getSimpleName().toString());
    }
    List<String> possibleNames;
    if (parameter.asType().getKind() == TypeKind.BOOLEAN) {
      possibleNames = Arrays.asList(name, "is" + upcase(name), "get" + upcase(name));
    } else {
      possibleNames = Arrays.asList(name, "get" + upcase(name));
    }
    for (String possibleName : possibleNames) {
      if (methods.containsKey(possibleName) &&
          TypeName.get(methods.get(possibleName).getReturnType()).equals(parameterType)) {
        return ProjectionMethod.create(possibleName, thrownTypes(methods.get(possibleName)));
      }
    }
    throw new ValidationException(MISSING_PROJECTION + name, parameter);
  }


  private static void validateType(RegularProjectableGoalElement goal,
                                   TypeElement type) {
    if (goal.executableElement.getKind() == CONSTRUCTOR
        && type.getModifiers().contains(ABSTRACT)) {
      throw new ValidationException(ABSTRACT_CONSTRUCTOR, goal.executableElement);
    }
  }

  static final Function<RegularGoalElement, SimpleRegularGoalDescription> validateBuilder
      = goal -> {
    List<TmpSimpleParameter> parameters = transform(executableElement.apply(goal).getParameters(),
        TmpSimpleParameter::create);
    List<TmpSimpleParameter> shuffled = shuffledParameters(parameters);
    List<TypeName> thrownTypes = thrownTypes(executableElement.apply(goal));
    return SimpleRegularGoalDescription.create(
        goal.details,
        thrownTypes,
        transform(shuffled, parameter -> parameter.parameter),
        goal.context);
  };

  private static ProjectedRegularGoalDescription createGoalDescription(RegularProjectableGoalElement goal,
                                                                       List<TmpProjectedParameter> parameters) {
    List<TmpProjectedParameter> shuffled = shuffledParameters(parameters);
    return ProjectedRegularGoalDescription.create(
        goal.details, thrownTypes(goal.executableElement),
        transform(shuffled, toValidParameter), goal.context);
  }

  private ProjectionValidatorV() {
    throw new UnsupportedOperationException("no instances");
  }
}
