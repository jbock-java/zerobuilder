package net.zerobuilder.compiler.analyse;

import com.squareup.javapoet.ClassName;
import net.zerobuilder.AccessLevel;
import net.zerobuilder.Builders;
import net.zerobuilder.Goal;
import net.zerobuilder.compiler.analyse.DtoGoalElement.AbstractGoalElement;
import net.zerobuilder.compiler.analyse.DtoGoalElement.BeanGoalElement;
import net.zerobuilder.compiler.analyse.DtoGoalElement.RegularGoalElement;
import net.zerobuilder.compiler.generate.DtoBuildersContext.BuildersContext;
import net.zerobuilder.compiler.generate.DtoGoalDescription.GoalDescription;
import net.zerobuilder.compiler.generate.GeneratorInput;

import javax.lang.model.element.Element;
import javax.lang.model.element.ElementKind;
import javax.lang.model.element.ExecutableElement;
import javax.lang.model.element.TypeElement;
import javax.lang.model.util.Elements;

import java.util.ArrayList;
import java.util.List;

import static javax.lang.model.element.ElementKind.CONSTRUCTOR;
import static javax.lang.model.element.ElementKind.METHOD;
import static javax.lang.model.element.Modifier.PRIVATE;
import static javax.tools.Diagnostic.Kind.WARNING;
import static net.zerobuilder.compiler.common.LessElements.asExecutable;
import static net.zerobuilder.compiler.Messages.ErrorMessages.NOT_ENOUGH_PARAMETERS;
import static net.zerobuilder.compiler.Messages.ErrorMessages.NO_GOALS;
import static net.zerobuilder.compiler.Messages.ErrorMessages.PRIVATE_METHOD;
import static net.zerobuilder.compiler.analyse.GoalnameValidator.checkNameConflict;
import static net.zerobuilder.compiler.analyse.ProjectionValidator.skip;
import static net.zerobuilder.compiler.analyse.ProjectionValidator.validate;
import static net.zerobuilder.compiler.analyse.TypeValidator.validateBuildersClass;
import static net.zerobuilder.compiler.analyse.Utilities.appendSuffix;
import static net.zerobuilder.compiler.generate.DtoBuildersContext.createBuildersContext;

public final class Analyser {

  private final Elements elements;

  public Analyser(Elements elements) {
    this.elements = elements;
  }

  public GeneratorInput analyse(TypeElement buildersAnnotatedClass) throws ValidationException {
    boolean recycle = buildersAnnotatedClass.getAnnotation(Builders.class).recycle();
    ClassName type = ClassName.get(buildersAnnotatedClass);
    ClassName generatedType = appendSuffix(type, "Builders");
    BuildersContext context = createBuildersContext(type, generatedType, recycle);
    List<AbstractGoalElement> goals = goals(buildersAnnotatedClass);
    checkNameConflict(goals);
    validateBuildersClass(buildersAnnotatedClass);
    List<GoalDescription> validGoals = new ArrayList<>();
    for (AbstractGoalElement goal : goals) {
      boolean toBuilder = goal.goalAnnotation.toBuilder();
      GoalDescription goalDescription = goal.accept(toBuilder ? validate : skip);
      validGoals.add(goalDescription);
    }
    return GeneratorInput.create(context, validGoals);
  }

  /**
   * @param buildElement a class that carries the {@link net.zerobuilder.Builders} annotation
   * @return the goals that this class defines: one per {@link Goal} annotation
   * @throws ValidationException if validation fails
   */
  private List<AbstractGoalElement> goals(TypeElement buildElement) throws ValidationException {
    Builders buildersAnnotation = buildElement.getAnnotation(Builders.class);
    List<AbstractGoalElement> builder = new ArrayList<>();
    AccessLevel defaultAccess = buildersAnnotation.access();
    if (buildElement.getAnnotation(Goal.class) != null) {
      builder.add(BeanGoalElement.create(buildElement, elements, defaultAccess));
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
          builder.add(RegularGoalElement.create(executableElement, elements, defaultAccess));
        }
      }
    }
    if (builder.isEmpty()) {
      throw new ValidationException(WARNING, NO_GOALS, buildElement);
    }
    return builder;
  }
}
