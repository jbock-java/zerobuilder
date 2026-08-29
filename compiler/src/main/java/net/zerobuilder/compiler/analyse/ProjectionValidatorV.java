package net.zerobuilder.compiler.analyse;

import com.palantir.javapoet.TypeName;
import java.util.List;
import java.util.Map;
import javax.lang.model.element.ExecutableElement;
import javax.lang.model.element.TypeElement;
import javax.lang.model.element.VariableElement;
import javax.lang.model.type.TypeKind;
import net.zerobuilder.compiler.analyse.ProjectionValidator.TmpProjectedParameter;
import net.zerobuilder.compiler.generate.DtoProjectionInfo.ProjectionInfo;
import net.zerobuilder.compiler.generate.GoalDescription;
import net.zerobuilder.compiler.generate.ProjectedParameter;

import static javax.lang.model.element.Modifier.PRIVATE;
import static javax.lang.model.element.Modifier.STATIC;
import static net.zerobuilder.compiler.Messages.MISSING_PROJECTION;
import static net.zerobuilder.compiler.analyse.ProjectionValidator.shuffledParameters;
import static net.zerobuilder.compiler.analyse.Utilities.thrownTypes;
import static net.zerobuilder.compiler.common.LessElements.getLocalFields;
import static net.zerobuilder.compiler.common.LessElements.getLocalMethods;
import static net.zerobuilder.compiler.common.LessTypes.asTypeElement;
import static net.zerobuilder.compiler.generate.DtoProjectionInfo.createFieldAccess;
import static net.zerobuilder.compiler.generate.DtoProjectionInfo.createGetterMethod;
import static net.zerobuilder.compiler.generate.GoalDescriptionFactory.createTheGoalDescription;

final class ProjectionValidatorV {

  private static boolean looksLikeAccessor(ExecutableElement method) {
    return method.getParameters().isEmpty()
        && !method.getModifiers().contains(PRIVATE)
        && !method.getModifiers().contains(STATIC)
        && method.getReturnType().getKind() != TypeKind.VOID
        && !"getClass".equals(method.getSimpleName().toString())
        && !"clone".equals(method.getSimpleName().toString());
  }

  static GoalDescription validateUpdater(GoalElement goal) {
    TypeElement tel = goal.details().tel();
    Map<String, ExecutableElement> methods = getLocalMethods(tel, ProjectionValidatorV::looksLikeAccessor);
    Map<String, VariableElement> fields = getLocalFields(tel);
    List<TmpProjectedParameter> parameters = goal.executableElement().getParameters().stream()
        .map(parameter -> ProjectedParameter.create(parameter, projectionInfo(methods, fields, parameter)))
        .map(TmpProjectedParameter::create)
        .toList();
    return createGoalDescription(goal, parameters);
  }

  private static ProjectionInfo projectionInfo(
      Map<String, ExecutableElement> methods,
      Map<String, VariableElement> fields,
      VariableElement parameter) {
    String name = parameter.getSimpleName().toString();
    TypeName parameterType = TypeName.get(parameter.asType());
    if (methods.containsKey(name) &&
        TypeName.get(methods.get(name).getReturnType()).equals(parameterType)) {
      return createGetterMethod(name, thrownTypes(methods.get(name)));
    }
    VariableElement field = fields.get(name);
    if (field != null && TypeName.get(field.asType()).equals(parameterType)) {
      return createFieldAccess(field.getSimpleName().toString());
    }
    throw new ValidationException(MISSING_PROJECTION + name, parameter);
  }

  private static GoalDescription createGoalDescription(
      GoalElement goal,
      List<TmpProjectedParameter> parameters) {
    List<TmpProjectedParameter> shuffled = shuffledParameters(parameters);
    return createTheGoalDescription(
        goal.details(),
        thrownTypes(goal.executableElement()),
        shuffled.stream().map(TmpProjectedParameter::parameter).toList(),
        goal.generatedType());
  }

  static void checkInheritance(TypeElement tel) {
    if (!tel.getInterfaces().isEmpty()) {
      throw new ValidationException("Interfaces are not allowed", tel);
    }
    TypeElement superclass = asTypeElement(tel.getSuperclass());
    if (superclass == null) {
      return;
    }
    if (superclass.getSuperclass().getKind() == TypeKind.NONE) {
      return;
    }
    if (superclass.getQualifiedName().contentEquals("java.lang.Record")) {
      return;
    }
    throw new ValidationException("Inheritance is not allowed", tel);
  }

  private ProjectionValidatorV() {
  }
}
