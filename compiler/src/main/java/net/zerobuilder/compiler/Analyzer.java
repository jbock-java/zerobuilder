package net.zerobuilder.compiler;

import com.google.common.collect.ImmutableList;
import com.squareup.javapoet.ClassName;
import com.squareup.javapoet.TypeName;
import net.zerobuilder.Build;
import net.zerobuilder.compiler.ToBuilderValidator.ProjectionInfo;

import javax.lang.model.element.ExecutableElement;
import javax.lang.model.element.TypeElement;
import javax.lang.model.util.Elements;

import static com.google.common.collect.Iterables.concat;
import static com.google.common.collect.Iterables.getOnlyElement;
import static javax.lang.model.element.ElementKind.CONSTRUCTOR;
import static javax.lang.model.element.Modifier.PRIVATE;
import static javax.lang.model.element.Modifier.STATIC;
import static javax.lang.model.util.ElementFilter.constructorsIn;
import static javax.lang.model.util.ElementFilter.methodsIn;
import static net.zerobuilder.compiler.BuildConfig.createBuildConfig;
import static net.zerobuilder.compiler.GoalContext.createGoalContext;
import static net.zerobuilder.compiler.Messages.ErrorMessages.COULD_NOT_GUESS_GOAL;
import static net.zerobuilder.compiler.Messages.ErrorMessages.NOT_ENOUGH_PARAMETERS;
import static net.zerobuilder.compiler.Messages.ErrorMessages.PRIVATE_METHOD;

final class Analyzer {

  private final TypeValidator typeValidator = new TypeValidator();
  private final ToBuilderValidator.Factory toBuilderValidatorFactory;

  Analyzer(Elements elements) {
    this.toBuilderValidatorFactory = new ToBuilderValidator.Factory(elements);
  }

  AnalysisResult parse(TypeElement buildElement) throws ValidationException {
    ImmutableList<ExecutableElement> goals = goals(buildElement);
    ClassName annotatedType = ClassName.get(buildElement);
    BuildConfig config = createBuildConfig(buildElement);
    ImmutableList.Builder<GoalContext> builder = ImmutableList.builder();
    for (ExecutableElement goal : goals) {
      TypeName goalType = goal.getKind() == CONSTRUCTOR
          ? annotatedType
          : TypeName.get(goal.getReturnType());
      typeValidator.validateBuildType(buildElement);
      ToBuilderValidator toBuilderValidator = toBuilderValidatorFactory
          .buildViaElement(goal).buildElement(buildElement);
      Build.Goal goalAnnotation = goal.getAnnotation(Build.Goal.class);
      boolean toBuilder = goalAnnotation != null && goalAnnotation.toBuilder();
      ImmutableList<ProjectionInfo> projectionInfos =
          toBuilder ? toBuilderValidator.validate() : toBuilderValidator.skip();
      builder.add(createGoalContext(goalType, config, projectionInfos, goal, toBuilder));
    }
    return new AnalysisResult(config, builder.build());
  }

  private ImmutableList<ExecutableElement> goals(TypeElement buildElement) throws ValidationException {
    ImmutableList.Builder<ExecutableElement> builder = ImmutableList.builder();
    for (ExecutableElement executableElement : concat(constructorsIn(buildElement.getEnclosedElements()),
        methodsIn(buildElement.getEnclosedElements()))) {
      if (executableElement.getAnnotation(Build.Goal.class) != null) {
        if (executableElement.getModifiers().contains(PRIVATE)) {
          throw new ValidationException(PRIVATE_METHOD, buildElement);
        }
        if (executableElement.getParameters().isEmpty()) {
          throw new ValidationException(NOT_ENOUGH_PARAMETERS, buildElement);
        }
        builder.add(executableElement);
      }
    }
    if (builder.build().isEmpty()) {
      return ImmutableList.of(guessGoal(buildElement));
    }
    return builder.build();
  }

  private ExecutableElement guessGoal(TypeElement buildElement) throws ValidationException {
    ImmutableList.Builder<ExecutableElement> builder = ImmutableList.builder();
    for (ExecutableElement executableElement : constructorsIn(buildElement.getEnclosedElements())) {
      if (!executableElement.getModifiers().contains(PRIVATE)
          && !executableElement.getParameters().isEmpty()) {
        builder.add(executableElement);
      }
    }
    if (builder.build().size() == 1) {
      return getOnlyElement(builder.build());
    }
    builder = ImmutableList.builder();
    for (ExecutableElement executableElement : methodsIn(buildElement.getEnclosedElements())) {
      if (!executableElement.getModifiers().contains(PRIVATE)
          && executableElement.getModifiers().contains(STATIC)
          && !executableElement.getParameters().isEmpty()) {
        builder.add(executableElement);
      }
    }
    if (builder.build().size() == 1) {
      return getOnlyElement(builder.build());
    }
    throw new ValidationException(COULD_NOT_GUESS_GOAL, buildElement);
  }

  static final class AnalysisResult {
    final BuildConfig config;
    final ImmutableList<GoalContext> goals;

    AnalysisResult(BuildConfig config, ImmutableList<GoalContext> goals) {
      this.config = config;
      this.goals = goals;
    }
  }

}
