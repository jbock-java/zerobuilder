package net.zerobuilder.compiler.analyse;

import com.squareup.javapoet.ClassName;
import net.zerobuilder.AccessLevel;
import net.zerobuilder.Builders;
import net.zerobuilder.Goal;
import net.zerobuilder.compiler.analyse.DtoGoalElement.AbstractGoalElement;
import net.zerobuilder.compiler.analyse.DtoGoalElement.BeanGoalElement;
import net.zerobuilder.compiler.analyse.DtoGoalElement.ModuleChoice;
import net.zerobuilder.compiler.generate.Builder;
import net.zerobuilder.compiler.generate.DtoContext.BuilderLifecycle;
import net.zerobuilder.compiler.generate.DtoContext.BuildersContext;
import net.zerobuilder.compiler.generate.DtoDescriptionInput.DescriptionInput;
import net.zerobuilder.compiler.generate.DtoDescriptionInput.ProjectedDescriptionInput;
import net.zerobuilder.compiler.generate.DtoDescriptionInput.SimpleDescriptionInput;
import net.zerobuilder.compiler.generate.DtoGeneratorInput.GeneratorInput;
import net.zerobuilder.compiler.generate.Updater;

import javax.lang.model.element.Element;
import javax.lang.model.element.ElementKind;
import javax.lang.model.element.ExecutableElement;
import javax.lang.model.element.TypeElement;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Function;

import static javax.lang.model.element.ElementKind.CONSTRUCTOR;
import static javax.lang.model.element.ElementKind.METHOD;
import static javax.lang.model.element.Modifier.PRIVATE;
import static javax.tools.Diagnostic.Kind.WARNING;
import static net.zerobuilder.compiler.Messages.ErrorMessages.NOT_ENOUGH_PARAMETERS;
import static net.zerobuilder.compiler.Messages.ErrorMessages.NO_GOALS;
import static net.zerobuilder.compiler.Messages.ErrorMessages.PRIVATE_METHOD;
import static net.zerobuilder.compiler.analyse.DtoGoalElement.goalElementCases;
import static net.zerobuilder.compiler.analyse.DtoGoalElement.goalName;
import static net.zerobuilder.compiler.analyse.DtoGoalElement.goalType;
import static net.zerobuilder.compiler.analyse.DtoGoalElement.module;
import static net.zerobuilder.compiler.analyse.DtoGoalElement.regularGoalElementCases;
import static net.zerobuilder.compiler.analyse.GoalnameValidator.checkNameConflict;
import static net.zerobuilder.compiler.analyse.ProjectionValidatorB.validateBean;
import static net.zerobuilder.compiler.analyse.ProjectionValidatorV.validateValue;
import static net.zerobuilder.compiler.analyse.ProjectionValidatorV.validateValueIgnoreProjections;
import static net.zerobuilder.compiler.analyse.TypeValidator.validateBuildersClass;
import static net.zerobuilder.compiler.analyse.Utilities.appendSuffix;
import static net.zerobuilder.compiler.analyse.Utilities.transform;
import static net.zerobuilder.compiler.common.LessElements.asExecutable;
import static net.zerobuilder.compiler.generate.DtoContext.createBuildersContext;

public final class Analyser {

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
    List<DescriptionInput> descriptions = transform(goalElements, createDescription);
    return GeneratorInput.create(context, descriptions);
  }

  private static final Function<AbstractGoalElement, DescriptionInput> createDescription =
      goalElementCases(
          regularGoalElementCases(
              regular -> new SimpleDescriptionInput(new Builder(), validateValueIgnoreProjections.apply(regular)),
              projectable -> new ProjectedDescriptionInput(new Updater(), validateValue.apply(projectable))),
          bean -> module.apply(bean) == ModuleChoice.BUILDER ?
              new SimpleDescriptionInput(new Builder(), validateBean.apply(bean)) :
              new ProjectedDescriptionInput(new Updater(), validateBean.apply(bean)));

  static List<ModuleChoice> modules(Goal goalAnnotation) {
    ArrayList<ModuleChoice> modules = new ArrayList<>(2);
    if (goalAnnotation.builder()) {
      modules.add(ModuleChoice.BUILDER);
    }
    if (goalAnnotation.updater()) {
      modules.add(ModuleChoice.UPDATER);
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
    } else {
      for (Element element : buildElement.getEnclosedElements()) {
        if (element.getAnnotation(Goal.class) != null) {
          ElementKind kind = element.getKind();
          if (kind == CONSTRUCTOR || kind == METHOD) {
            ExecutableElement executableElement = asExecutable(element);
            validateExecutable(buildElement, executableElement);
            builder.addAll(DtoGoalElement.createRegular(executableElement, defaultAccess));
          }
        }
      }
    }
    if (builder.isEmpty()) {
      throw new ValidationException(WARNING, NO_GOALS, buildElement);
    }
    return builder;
  }
  private static void validateExecutable(TypeElement buildElement, ExecutableElement executableElement) {
    if (executableElement.getModifiers().contains(PRIVATE)) {
      throw new ValidationException(PRIVATE_METHOD, buildElement);
    }
    if (executableElement.getParameters().isEmpty()) {
      throw new ValidationException(NOT_ENOUGH_PARAMETERS, buildElement);
    }
  }

  private Analyser() {
    throw new UnsupportedOperationException("no instances");
  }
}
