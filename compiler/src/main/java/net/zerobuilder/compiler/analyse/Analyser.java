package net.zerobuilder.compiler.analyse;

import com.google.common.collect.ImmutableList;
import net.zerobuilder.Goal;
import net.zerobuilder.compiler.analyse.DtoPackage.GoalTypes.AbstractGoalElement;
import net.zerobuilder.compiler.analyse.DtoPackage.GoalTypes.BeanGoalElement;
import net.zerobuilder.compiler.analyse.DtoPackage.GoalTypes.RegularGoalElement;
import net.zerobuilder.compiler.analyse.DtoShared.AnalysisResult;
import net.zerobuilder.compiler.analyse.DtoShared.ValidGoal;
import net.zerobuilder.compiler.generate.BuildersType;
import net.zerobuilder.compiler.generate.GoalContext.AbstractContext;

import javax.lang.model.element.Element;
import javax.lang.model.element.ElementKind;
import javax.lang.model.element.ExecutableElement;
import javax.lang.model.element.TypeElement;
import javax.lang.model.util.Elements;

import static com.google.auto.common.MoreElements.asExecutable;
import static javax.lang.model.element.ElementKind.CONSTRUCTOR;
import static javax.lang.model.element.ElementKind.METHOD;
import static javax.lang.model.element.Modifier.PRIVATE;
import static javax.tools.Diagnostic.Kind.WARNING;
import static net.zerobuilder.compiler.Messages.ErrorMessages.NOT_ENOUGH_PARAMETERS;
import static net.zerobuilder.compiler.Messages.ErrorMessages.NO_GOALS;
import static net.zerobuilder.compiler.Messages.ErrorMessages.PRIVATE_METHOD;
import static net.zerobuilder.compiler.analyse.GoalContextFactory.context;
import static net.zerobuilder.compiler.analyse.GoalnameValidator.checkNameConflict;
import static net.zerobuilder.compiler.analyse.ProjectionValidator.skip;
import static net.zerobuilder.compiler.analyse.ProjectionValidator.validate;
import static net.zerobuilder.compiler.analyse.TypeValidator.validateBuildersType;
import static net.zerobuilder.compiler.generate.BuildersType.createBuilderContext;

public final class Analyser {

  private final Elements elements;

  public Analyser(Elements elements) {
    this.elements = elements;
  }

  public AnalysisResult analyse(TypeElement buildElement) throws ValidationException {
    BuildersType context = createBuilderContext(buildElement);
    ImmutableList.Builder<AbstractContext> builder = ImmutableList.builder();
    ImmutableList<AbstractGoalElement> goals = goals(buildElement);
    checkNameConflict(goals);
    for (AbstractGoalElement goal : goals) {
      validateBuildersType(buildElement);
      boolean toBuilder = goal.goalAnnotation.toBuilder();
      boolean isBuilder = goal.goalAnnotation.builder();
      ValidGoal validGoal = goal.accept(toBuilder ? validate : skip);
      builder.add(context(validGoal, context, toBuilder, isBuilder));
    }
    return new AnalysisResult(context, builder.build());
  }

  /**
   * @param buildElement a class that carries the {@link net.zerobuilder.Builders} annotation
   * @return the goals that this class defines: one per {@link Goal} annotation
   * @throws ValidationException if validation fails
   */
  private ImmutableList<AbstractGoalElement> goals(TypeElement buildElement) throws ValidationException {
    ImmutableList.Builder<AbstractGoalElement> builder = ImmutableList.builder();
    if (buildElement.getAnnotation(Goal.class) != null) {
      builder.add(BeanGoalElement.create(buildElement, elements));
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
          builder.add(RegularGoalElement.create(executableElement, elements));
        }
      }
    }
    ImmutableList<AbstractGoalElement> goals = builder.build();
    if (goals.isEmpty()) {
      throw new ValidationException(WARNING, NO_GOALS, buildElement);
    }
    return goals;
  }
}
