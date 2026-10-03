package net.zerobuilder.compiler.analyse;

import com.palantir.javapoet.ClassName;
import java.util.List;
import javax.lang.model.element.ExecutableElement;
import javax.lang.model.element.TypeElement;
import javax.lang.model.type.TypeKind;
import javax.lang.model.type.TypeMirror;
import net.zerobuilder.compiler.common.LessElements;
import net.zerobuilder.compiler.common.LessTypes;
import net.zerobuilder.compiler.generate.GoalDescription;

import static javax.lang.model.element.ElementKind.CONSTRUCTOR;
import static javax.lang.model.element.Modifier.PRIVATE;
import static net.zerobuilder.compiler.Messages.CHECKED;
import static net.zerobuilder.compiler.Messages.PRIVATE_METHOD;
import static net.zerobuilder.compiler.analyse.ProjectionValidatorV.checkInheritance;
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
    checkNoChecked(constructor);
    GoalElement goal = GoalElement.create(tel, constructor, generatedType);
    return ProjectionValidatorV.validateUpdater(goal);
  }

  public static void checkNoChecked(ExecutableElement el) throws ValidationException {
    for (TypeMirror thrownType : el.getThrownTypes()) {
      if (isChecked(thrownType)) {
        throw new ValidationException(CHECKED, el);
      }
    }
  }

  private static boolean isChecked(TypeMirror typeMirror) {
    TypeMirror check = typeMirror;
    while (true) {
      if (check == null) {
        return false;
      }
      if (check.getKind() == TypeKind.NONE) {
        return false;
      }
      TypeElement tel = LessTypes.asTypeElement(check);
      if (tel == null) {
        return false;
      }
      if (tel.getQualifiedName().contentEquals("java.lang.RuntimeException")) {
        return false;
      }
      if (tel.getQualifiedName().contentEquals("java.lang.Exception")) {
        return true;
      }
      check = tel.getSuperclass();
    }
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
