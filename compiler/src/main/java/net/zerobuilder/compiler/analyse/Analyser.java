package net.zerobuilder.compiler.analyse;

import com.palantir.javapoet.ClassName;
import net.zerobuilder.compiler.common.LessElements;
import net.zerobuilder.compiler.generate.GoalDescription;

import javax.lang.model.element.ExecutableElement;
import javax.lang.model.element.Modifier;
import javax.lang.model.element.TypeElement;
import java.util.List;

import static javax.lang.model.element.ElementKind.CONSTRUCTOR;
import static net.zerobuilder.compiler.Messages.PRIVATE_METHOD;
import static net.zerobuilder.compiler.analyse.ProjectionValidatorV.validateUpdater;
import static net.zerobuilder.compiler.analyse.TypeValidator.validateContextClass;
import static net.zerobuilder.compiler.analyse.Utilities.peer;

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
    ClassName generatedType = peer(ClassName.get(tel), "Builders");
    ExecutableElement constructor = getConstructor(tel);
    checkAccessLevel(constructor);
    GoalElement goal = DtoGoalElement.create(tel, constructor, generatedType);
    return validateUpdater(goal);
  }

  private static ExecutableElement getConstructor(TypeElement tel) {
    List<ExecutableElement> constructors = tel.getEnclosedElements().stream().filter(el -> el.getKind() == CONSTRUCTOR).map(LessElements::asExecutable).toList();
    if (constructors.isEmpty()) {
      throw new ValidationException("constructor not found", tel);
    }
    if (constructors.size() >= 2) {
      throw new ValidationException("more than one constructor found", tel);
    }
    return constructors.getFirst();
  }

  private static void checkAccessLevel(ExecutableElement constructor) throws ValidationException {
    if (constructor.getModifiers().contains(Modifier.PRIVATE)) {
      throw new ValidationException(PRIVATE_METHOD, constructor);
    }
  }

  private Analyser() {
  }
}
