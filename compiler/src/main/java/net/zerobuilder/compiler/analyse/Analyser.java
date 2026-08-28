package net.zerobuilder.compiler.analyse;

import com.palantir.javapoet.ClassName;
import java.util.List;
import javax.lang.model.element.ExecutableElement;
import javax.lang.model.element.TypeElement;
import net.zerobuilder.compiler.common.LessElements;
import net.zerobuilder.compiler.generate.GoalDescription;

import static javax.lang.model.element.ElementKind.CONSTRUCTOR;
import static javax.lang.model.element.Modifier.PRIVATE;
import static net.zerobuilder.compiler.Messages.PRIVATE_METHOD;
import static net.zerobuilder.compiler.analyse.ProjectionValidatorV.checkInheritance;
import static net.zerobuilder.compiler.analyse.ProjectionValidatorV.validateUpdater;
import static net.zerobuilder.compiler.analyse.TypeValidator.validateContextClass;
import static net.zerobuilder.compiler.analyse.Utilities.peer;

public final class Analyser {

  /**
   * Determine goal from the given type by inspecting its annotations.
   *
   * @param tel a type element
   * @return goal description
   * @throws ValidationException if validation fails
   */
  public static GoalDescription analyse(TypeElement tel) throws ValidationException {
    validateContextClass(tel);
    checkInheritance(tel);
    ClassName generatedType = peer(ClassName.get(tel), "Builders");
    ExecutableElement constructor = getConstructor(tel);
    checkAccessLevel(constructor);
    GoalElement goal = GoalElement.create(tel, constructor, generatedType);
    return validateUpdater(goal);
  }

  private static ExecutableElement getConstructor(TypeElement tel) {
    List<ExecutableElement> constructors = tel.getEnclosedElements().stream()
        .filter(el -> el.getKind() == CONSTRUCTOR)
        .map(LessElements::asExecutable)
        .toList();
    if (constructors.isEmpty()) {
      throw new ValidationException("constructor not found", tel);
    }
    if (constructors.size() >= 2) {
      throw new ValidationException("more than one constructor found", tel);
    }
    return constructors.getFirst();
  }

  private static void checkAccessLevel(ExecutableElement constructor) throws ValidationException {
    if (constructor.getModifiers().contains(PRIVATE)) {
      throw new ValidationException(PRIVATE_METHOD, constructor);
    }
  }

  private Analyser() {
  }
}
