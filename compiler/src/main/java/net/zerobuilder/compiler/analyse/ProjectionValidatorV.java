package net.zerobuilder.compiler.analyse;

import com.palantir.javapoet.TypeName;
import java.util.List;
import java.util.Map;
import javax.lang.model.element.ExecutableElement;
import javax.lang.model.element.TypeElement;
import javax.lang.model.element.VariableElement;
import javax.lang.model.type.TypeKind;
import javax.lang.model.type.TypeMirror;
import net.zerobuilder.compiler.analyse.ProjectionValidator.TmpProjectedParameter;
import net.zerobuilder.compiler.generate.DtoProjectionInfo.ProjectionInfo;
import net.zerobuilder.compiler.generate.GoalDescription;

import static javax.lang.model.element.ElementKind.CONSTRUCTOR;
import static javax.lang.model.element.Modifier.PRIVATE;
import static javax.lang.model.element.Modifier.STATIC;
import static net.zerobuilder.compiler.Messages.ErrorMessages.MISSING_PROJECTION;
import static net.zerobuilder.compiler.analyse.ProjectionValidator.TmpProjectedParameter.toValidParameter;
import static net.zerobuilder.compiler.analyse.ProjectionValidator.shuffledParameters;
import static net.zerobuilder.compiler.analyse.Utilities.thrownTypes;
import static net.zerobuilder.compiler.common.LessElements.getLocalAndInheritedFields;
import static net.zerobuilder.compiler.common.LessElements.getLocalAndInheritedMethods;
import static net.zerobuilder.compiler.common.LessTypes.asTypeElement;
import static net.zerobuilder.compiler.common.LessTypes.isDeclaredType;
import static net.zerobuilder.compiler.generate.DtoProjectionInfo.createFieldAccess;
import static net.zerobuilder.compiler.generate.DtoProjectionInfo.createGetterMethod;
import static net.zerobuilder.compiler.generate.DtoRegularGoalDescription.createUpdaterGoalDescription;
import static net.zerobuilder.compiler.generate.ZeroUtil.transform;
import static net.zerobuilder.compiler.generate.ZeroUtil.upcase;

final class ProjectionValidatorV {

  private static boolean looksLikeGetter(ExecutableElement method) {
    return method.getParameters().isEmpty()
        && !method.getModifiers().contains(PRIVATE)
        && !method.getModifiers().contains(STATIC)
        && method.getReturnType().getKind() != TypeKind.VOID
        && !"getClass".equals(method.getSimpleName().toString())
        && !"clone".equals(method.getSimpleName().toString());
  }

  static GoalDescription validateUpdater(GoalElement goal) {
    TypeMirror mirror = goal.executableElement().getKind() == CONSTRUCTOR ?
        goal.executableElement().getEnclosingElement().asType() :
        goal.executableElement().getReturnType();
    if (!isDeclaredType(mirror)) {
      return createGoalDescription(goal, List.of());
    }
    TypeElement type = asTypeElement(mirror);
    Map<String, ExecutableElement> methods = getLocalAndInheritedMethods(type, ProjectionValidatorV::looksLikeGetter);
    Map<String, VariableElement> fields = getLocalAndInheritedFields(type);
    List<TmpProjectedParameter> parameters = transform(goal.executableElement().getParameters(),
        parameter -> TmpProjectedParameter.create(parameter,
            projectionInfo(methods, fields, parameter)));
    return createGoalDescription(goal, parameters);
  }

  private static ProjectionInfo projectionInfo(
      Map<String, ExecutableElement> methods,
      Map<String, VariableElement> fields,
      VariableElement parameter) {
    String name = parameter.getSimpleName().toString();
    VariableElement field = fields.get(name);
    TypeName parameterType = TypeName.get(parameter.asType());
    if (field != null && TypeName.get(field.asType()).equals(parameterType)) {
      return createFieldAccess(field.getSimpleName().toString());
    }
    List<String> possibleNames;
    if (parameter.asType().getKind() == TypeKind.BOOLEAN) {
      possibleNames = List.of(name, "is" + upcase(name), "get" + upcase(name));
    } else {
      possibleNames = List.of(name, "get" + upcase(name));
    }
    for (String possibleName : possibleNames) {
      if (methods.containsKey(possibleName) &&
          TypeName.get(methods.get(possibleName).getReturnType()).equals(parameterType)) {
        return createGetterMethod(possibleName, thrownTypes(methods.get(possibleName)));
      }
    }
    throw new ValidationException(MISSING_PROJECTION + name, parameter);
  }

  private static GoalDescription createGoalDescription(
      GoalElement goal,
      List<TmpProjectedParameter> parameters) {
    List<TmpProjectedParameter> shuffled = shuffledParameters(parameters);
    return createUpdaterGoalDescription(
        goal.details(),
        thrownTypes(goal.executableElement()),
        transform(shuffled, toValidParameter), goal.generatedType());
  }

  private ProjectionValidatorV() {
  }
}
