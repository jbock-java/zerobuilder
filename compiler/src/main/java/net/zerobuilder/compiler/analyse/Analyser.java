package net.zerobuilder.compiler.analyse;

import com.squareup.javapoet.ClassName;
import net.zerobuilder.AccessLevel;
import net.zerobuilder.Builders;
import net.zerobuilder.Goal;
import net.zerobuilder.compiler.analyse.DtoGoalElement.AbstractGoalElement;
import net.zerobuilder.compiler.analyse.DtoGoalElement.BeanGoalElement;
import net.zerobuilder.compiler.analyse.DtoGoalElement.RegularGoalElement;
import net.zerobuilder.compiler.generate.Builder;
import net.zerobuilder.compiler.generate.DtoContext.BuilderLifecycle;
import net.zerobuilder.compiler.generate.DtoContext.BuildersContext;
import net.zerobuilder.compiler.generate.DtoDescriptionInput;
import net.zerobuilder.compiler.generate.DtoDescriptionInput.DescriptionInput;
import net.zerobuilder.compiler.generate.DtoDescriptionInput.SimpleDescriptionInput;
import net.zerobuilder.compiler.generate.DtoGeneratorInput.GeneratorInput;
import net.zerobuilder.compiler.generate.DtoModule.Module;
import net.zerobuilder.compiler.generate.Updater;

import javax.lang.model.element.Element;
import javax.lang.model.element.ElementKind;
import javax.lang.model.element.ExecutableElement;
import javax.lang.model.element.TypeElement;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

import static java.util.stream.Collectors.toList;
import static javax.lang.model.element.ElementKind.CONSTRUCTOR;
import static javax.lang.model.element.ElementKind.METHOD;
import static javax.lang.model.element.Modifier.PRIVATE;
import static javax.tools.Diagnostic.Kind.WARNING;
import static net.zerobuilder.compiler.Messages.ErrorMessages.NOT_ENOUGH_PARAMETERS;
import static net.zerobuilder.compiler.Messages.ErrorMessages.NO_GOALS;
import static net.zerobuilder.compiler.Messages.ErrorMessages.PRIVATE_METHOD;
import static net.zerobuilder.compiler.analyse.DtoGoalElement.goalName;
import static net.zerobuilder.compiler.analyse.DtoGoalElement.goalType;
import static net.zerobuilder.compiler.analyse.GoalnameValidator.checkNameConflict;
import static net.zerobuilder.compiler.analyse.ProjectionValidator.skip;
import static net.zerobuilder.compiler.analyse.ProjectionValidator.validate;
import static net.zerobuilder.compiler.analyse.TypeValidator.validateBuildersClass;
import static net.zerobuilder.compiler.analyse.Utilities.appendSuffix;
import static net.zerobuilder.compiler.analyse.Utilities.transform;
import static net.zerobuilder.compiler.common.LessElements.asExecutable;
import static net.zerobuilder.compiler.generate.DtoContext.createBuildersContext;

public final class Analyser {

  private static final Builder MODULE_BUILDER = new Builder();
  private static final Updater MODULE_UPDATER = new Updater();

  // only for validation
  static final class NameElement {
    final String name;
    final Element element;
    final Goal goalAnnotation;
    NameElement(String name, Element element, Goal goalAnnotation) {
      this.name = name;
      this.element = element;
      this.goalAnnotation = goalAnnotation;
    }
  }

  public static GeneratorInput analyse(TypeElement buildersAnnotatedClass) throws ValidationException {
    BuilderLifecycle lifecycle = buildersAnnotatedClass.getAnnotation(Builders.class).recycle()
        ? BuilderLifecycle.REUSE_INSTANCES
        : BuilderLifecycle.NEW_INSTANCE;
    ClassName type = ClassName.get(buildersAnnotatedClass);
    ClassName generatedType = appendSuffix(type, "Builders");
    BuildersContext context = createBuildersContext(type, generatedType, lifecycle);
    List<AbstractGoalElement> goalElements = goals(buildersAnnotatedClass);
    checkNameConflict(names(buildersAnnotatedClass));
    validateBuildersClass(buildersAnnotatedClass);
    List<DescriptionInput> descriptions = transform(goalElements, goalElement ->
        new SimpleDescriptionInput(
            goalElement.module,
            goalElement.goalAnnotation.updater() ?
                validate.apply(goalElement) :
                skip.apply(goalElement)));
    return GeneratorInput.create(context, descriptions);
  }

  static List<? extends Module> modules(Goal goalAnnotation) {
    ArrayList<Module> modules = new ArrayList<>(2);
    if (goalAnnotation.builder()) {
      modules.add(MODULE_BUILDER);
    }
    if (goalAnnotation.updater()) {
      modules.add(MODULE_UPDATER);
    }
    return modules;
  }

  private static List<NameElement> names(TypeElement buildElement) throws ValidationException {
    List<NameElement> builder = new ArrayList<>();
    if (buildElement.getAnnotation(Goal.class) != null) {
      ClassName goalType = ClassName.get(buildElement);
      Goal goalAnnotation = buildElement.getAnnotation(Goal.class);
      String name = goalName(goalAnnotation, goalType);
      builder.add(new NameElement(name, buildElement, goalAnnotation));
    }
    for (Element element : buildElement.getEnclosedElements()) {
      Goal goalAnnotation = element.getAnnotation(Goal.class);
      if (goalAnnotation != null) {
        ElementKind kind = element.getKind();
        if (kind == CONSTRUCTOR || kind == METHOD) {
          ExecutableElement executableElement = asExecutable(element);
          String name = goalName(goalAnnotation, goalType(executableElement));
          builder.add(new NameElement(name, executableElement, goalAnnotation));
        }
      }
    }
    return builder;
  }


  /**
   * @param buildElement a class that carries the {@link net.zerobuilder.Builders} annotation
   * @return the goals that this class defines: one per {@link Goal} annotation
   * @throws ValidationException if validation fails
   */
  private static List<AbstractGoalElement> goals(TypeElement buildElement) throws ValidationException {
    Builders buildersAnnotation = buildElement.getAnnotation(Builders.class);
    List<AbstractGoalElement> builder = new ArrayList<>();
    AccessLevel defaultAccess = buildersAnnotation.access();
    if (buildElement.getAnnotation(Goal.class) != null) {
      builder.addAll(BeanGoalElement.create(buildElement, defaultAccess));
    }
    for (Element element : buildElement.getEnclosedElements()) {
      if (element.getAnnotation(Goal.class) != null) {
        ElementKind kind = element.getKind();
        if (kind == CONSTRUCTOR || kind == METHOD) {
          ExecutableElement executableElement = asExecutable(element);
          if (executableElement.getModifiers().contains(PRIVATE)) {
            throw new ValidationException(PRIVATE_METHOD, buildElement);
          }
          if (executableElement.getParameters().isEmpty()) {
            throw new ValidationException(NOT_ENOUGH_PARAMETERS, buildElement);
          }
          builder.addAll(RegularGoalElement.create(executableElement, defaultAccess));
        }
      }
    }
    if (builder.isEmpty()) {
      throw new ValidationException(WARNING, NO_GOALS, buildElement);
    }
    return builder;
  }

  private Analyser() {
    throw new UnsupportedOperationException("no instances");
  }
}
