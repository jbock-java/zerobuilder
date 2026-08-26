package net.zerobuilder.compiler.analyse;

import com.palantir.javapoet.ClassName;
import com.palantir.javapoet.TypeName;
import com.palantir.javapoet.TypeVariableName;
import java.util.List;
import javax.lang.model.element.TypeElement;
import net.zerobuilder.Builder;
import net.zerobuilder.RecordBuilder;
import net.zerobuilder.compiler.common.LessElements;
import net.zerobuilder.compiler.generate.GoalDescription;
import net.zerobuilder.compiler.generate.GoalContext;

import static javax.lang.model.element.ElementKind.CONSTRUCTOR;
import static net.zerobuilder.compiler.analyse.DtoGoalElement.createBuilderGoal;
import static net.zerobuilder.compiler.analyse.DtoGoalElement.parameterNames;
import static net.zerobuilder.compiler.analyse.MoreValidations.checkAccessLevel;
import static net.zerobuilder.compiler.analyse.ProjectionValidatorV.validateUpdater;
import static net.zerobuilder.compiler.analyse.TypeValidator.validateContextClass;
import static net.zerobuilder.compiler.analyse.Utilities.peer;
import static net.zerobuilder.compiler.common.LessTypes.asTypeElement;
import static net.zerobuilder.compiler.generate.ZeroUtil.parameterizedTypeName;
import static net.zerobuilder.compiler.generate.ZeroUtil.rawClassName;
import static net.zerobuilder.compiler.generate.ZeroUtil.transform;

public final class Analyser {

  /**
   * Extract all goals from the given type, by inspecting annotations.
   * Perform validations and bundle each goal with the appropriate module.
   *
   * @param tel a type element
   * @return list of goal inputs
   * @throws ValidationException if validation fails
   */
  public static GoalDescription analyse(TypeElement tel) throws ValidationException {
    validateContextClass(tel);
    TypeName type = parameterizedTypeName(ClassName.get(tel),
        transform(tel.getTypeParameters(), TypeVariableName::get));
    ClassName generatedType = peer(rawClassName(type), "Builders");
    GoalContext context = new GoalContext(type, generatedType);
    GoalElement goal = createGoalElement(tel, context);
    checkAccessLevel(goal);
    return validateUpdater(goal);
  }

  private static GoalElement createGoalElement(
      TypeElement tel,
      GoalContext context) {
    RecordBuilder recordBuilderAnnotation = tel.getAnnotation(RecordBuilder.class);
    if (recordBuilderAnnotation != null) {
      TypeElement superclass = asTypeElement(tel.getSuperclass());
      if (!superclass.getQualifiedName().contentEquals("java.lang.Record")) {
        throw new ValidationException("Not a record type", tel);
      }
      return tel.getEnclosedElements().stream()
          .filter(el -> el.getKind() == CONSTRUCTOR)
          .map(LessElements::asExecutable)
          .map(element -> createBuilderGoal(element, GoalModifiers.create(element), parameterNames(element), context))
          .findFirst().orElseThrow(() -> new ValidationException("constructor not found", tel));
    }
    List<GoalElement> result = tel.getEnclosedElements().stream()
        .filter(el -> el.getAnnotation(Builder.class) != null)
        .map(LessElements::asExecutable)
        .map(element -> createBuilderGoal(element, GoalModifiers.create(element), parameterNames(element), context))
        .toList();
    if (result.size() >= 2) {
      throw new ValidationException("Found more than one annotated constructor", tel);
    }
    return result.stream().findFirst().orElseThrow(() -> new ValidationException("annotated constructor not found", tel));
  }

  private Analyser() {
  }
}
