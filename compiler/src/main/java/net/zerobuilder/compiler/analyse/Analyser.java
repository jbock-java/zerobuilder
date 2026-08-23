package net.zerobuilder.compiler.analyse;

import com.palantir.javapoet.ClassName;
import com.palantir.javapoet.TypeName;
import com.palantir.javapoet.TypeVariableName;
import java.util.ArrayList;
import java.util.List;
import javax.lang.model.element.ExecutableElement;
import javax.lang.model.element.TypeElement;
import net.zerobuilder.Builder;
import net.zerobuilder.RecordBuilder;
import net.zerobuilder.RecordUpdater;
import net.zerobuilder.Updater;
import net.zerobuilder.compiler.analyse.DtoGoalElement.AbstractGoalElement;
import net.zerobuilder.compiler.analyse.DtoGoalElement.ModuleChoice;
import net.zerobuilder.compiler.analyse.DtoGoalElement.BuilderGoalElement;
import net.zerobuilder.compiler.analyse.DtoGoalElement.UpdaterGoalElement;
import net.zerobuilder.compiler.common.LessElements;
import net.zerobuilder.compiler.generate.DtoGeneratorInput.AbstractGoalInput;
import net.zerobuilder.compiler.generate.DtoGeneratorInput.UpdaterGoalInput;
import net.zerobuilder.compiler.generate.DtoGeneratorInput.BuilderGoalInput;
import net.zerobuilder.compiler.generate.DtoModule.BuilderModule;
import net.zerobuilder.compiler.generate.DtoModule.UpdaterModule;
import net.zerobuilder.compiler.generate.GoalContext;
import net.zerobuilder.modules.builder.RegularBuilder;
import net.zerobuilder.modules.generics.GenericsBuilder;
import net.zerobuilder.modules.updater.RegularUpdater;

import static javax.lang.model.element.ElementKind.CONSTRUCTOR;
import static javax.lang.model.element.ElementKind.METHOD;
import static javax.lang.model.element.Modifier.STATIC;
import static net.zerobuilder.compiler.analyse.DtoGoalElement.createRegular;
import static net.zerobuilder.compiler.analyse.MoreValidations.checkAccessLevel;
import static net.zerobuilder.compiler.analyse.MoreValidations.checkNameConflict;
import static net.zerobuilder.compiler.analyse.ProjectionValidatorV.validateBuilder;
import static net.zerobuilder.compiler.analyse.ProjectionValidatorV.validateUpdater;
import static net.zerobuilder.compiler.analyse.TypeValidator.validateContextClass;
import static net.zerobuilder.compiler.analyse.Utilities.peer;
import static net.zerobuilder.compiler.common.LessTypes.asTypeElement;
import static net.zerobuilder.compiler.generate.ZeroUtil.parameterizedTypeName;
import static net.zerobuilder.compiler.generate.ZeroUtil.rawClassName;
import static net.zerobuilder.compiler.generate.ZeroUtil.transform;

public final class Analyser {

  private static final BuilderModule BUILDER = new RegularBuilder();
  private static final UpdaterModule UPDATER = new RegularUpdater();
  private static final BuilderModule GENERICS = new GenericsBuilder();

  /**
   * Extract all goals from the given type, by inspecting annotations.
   * Perform validations and bundle each goal with the appropriate module.
   *
   * @param tel a type element
   * @return list of goal inputs
   * @throws ValidationException if validation fails
   */
  public static List<AbstractGoalInput> analyse(TypeElement tel) throws ValidationException {
    validateContextClass(tel);
    TypeName type = parameterizedTypeName(ClassName.get(tel),
        transform(tel.getTypeParameters(), TypeVariableName::get));
    ClassName generatedType = peer(rawClassName(type), "Builders");
    GoalContext context = new GoalContext(type, generatedType);
    List<? extends AbstractGoalElement> goals = regularGoals(tel, context);
    checkNameConflict(goals);
    checkAccessLevel(goals);
    return transform(goals, Analyser::assignModule);
  }

  private static AbstractGoalInput assignModule(AbstractGoalElement element) {
    return switch (element) {
      case BuilderGoalElement regular -> hasTypevars(regular.executableElement()) ?
          new BuilderGoalInput(GENERICS, validateBuilder(regular)) :
          new BuilderGoalInput(BUILDER, validateBuilder(regular));
      case UpdaterGoalElement projected -> new UpdaterGoalInput(UPDATER, validateUpdater(projected));
    };
  }

  private static boolean hasTypevars(ExecutableElement element) {
    return !element.getTypeParameters().isEmpty()
        || !element.getModifiers().contains(STATIC)
        && !asTypeElement(element.getEnclosingElement().asType()).getTypeParameters().isEmpty();
  }

  private static List<? extends AbstractGoalElement> regularGoals(TypeElement tel, GoalContext context) {
    RecordBuilder recordBuilderAnnotation = tel.getAnnotation(RecordBuilder.class);
    RecordUpdater recordUpdaterAnnotation = tel.getAnnotation(RecordUpdater.class);
    if (recordBuilderAnnotation != null || recordUpdaterAnnotation != null) {
      TypeElement superclass = asTypeElement(tel.getSuperclass());
      if (!superclass.getQualifiedName().contentEquals("java.lang.Record")) {
        throw new ValidationException("Not a record type", tel);
      }
      List<ModuleChoice> options = new ArrayList<>(2);
      if (recordBuilderAnnotation != null) {
        options.add(ModuleChoice.BUILDER);
      }
      if (recordUpdaterAnnotation != null) {
        options.add(ModuleChoice.UPDATER);
      }
      return tel.getEnclosedElements().stream()
          .filter(el -> el.getKind() == CONSTRUCTOR)
          .map(LessElements::asExecutable)
          .map(element -> createRegular(context, element, options))
          .flatMap(List::stream)
          .toList();
    }
    return tel.getEnclosedElements().stream()
        .filter(el -> el.getAnnotation(Builder.class) != null || el.getAnnotation(Updater.class) != null)
        .filter(el -> el.getKind() == CONSTRUCTOR || el.getKind() == METHOD)
        .map(LessElements::asExecutable)
        .map(element -> createRegular(context, element, goalOptions(element)))
        .flatMap(List::stream)
        .toList();
  }

  private static List<ModuleChoice> goalOptions(ExecutableElement element) {
    List<ModuleChoice> options = new ArrayList<>(2);
    if (element.getAnnotation(Builder.class) != null) {
      options.add(ModuleChoice.BUILDER);
    }
    if (element.getAnnotation(Updater.class) != null) {
      options.add(ModuleChoice.UPDATER);
    }
    return options;
  }

  private Analyser() {
    throw new UnsupportedOperationException("no instances");
  }
}
