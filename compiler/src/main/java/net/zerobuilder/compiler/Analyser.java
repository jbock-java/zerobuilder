package net.zerobuilder.compiler;

import com.google.common.base.Predicate;
import com.google.common.collect.FluentIterable;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.Ordering;
import com.google.common.primitives.Ints;
import com.squareup.javapoet.ClassName;
import com.squareup.javapoet.TypeName;
import net.zerobuilder.Goal;
import net.zerobuilder.compiler.ToBuilderValidator.ProjectionInfo;

import javax.lang.model.element.ExecutableElement;
import javax.lang.model.element.TypeElement;
import javax.lang.model.util.Elements;
import java.util.Comparator;
import java.util.HashMap;

import static com.google.common.base.Strings.isNullOrEmpty;
import static com.google.common.collect.Iterables.concat;
import static com.google.common.collect.Iterables.getOnlyElement;
import static javax.lang.model.element.ElementKind.CONSTRUCTOR;
import static javax.lang.model.element.ElementKind.METHOD;
import static javax.lang.model.element.Modifier.PRIVATE;
import static javax.lang.model.element.Modifier.STATIC;
import static javax.lang.model.util.ElementFilter.constructorsIn;
import static javax.lang.model.util.ElementFilter.methodsIn;
import static net.zerobuilder.compiler.BuildConfig.createBuildConfig;
import static net.zerobuilder.compiler.GoalContext.createGoalContext;
import static net.zerobuilder.compiler.Messages.ErrorMessages.COULD_NOT_GUESS_GOAL;
import static net.zerobuilder.compiler.Messages.ErrorMessages.MULTIPLE_TOBUILDER;
import static net.zerobuilder.compiler.Messages.ErrorMessages.NOT_ENOUGH_PARAMETERS;
import static net.zerobuilder.compiler.Messages.ErrorMessages.PRIVATE_METHOD;

final class Analyser {

  private static final Ordering<GoalContext> CONSTRUCTORS_FIRST = Ordering.from(new Comparator<GoalContext>() {
    @Override
    public int compare(GoalContext g0, GoalContext g1) {
      int w0 = g0.innerContext.goal.getKind() == CONSTRUCTOR ? 0 : 1;
      int w1 = g1.innerContext.goal.getKind() == CONSTRUCTOR ? 0 : 1;
      return Ints.compare(w0, w1);
    }
  });
  private final TypeValidator typeValidator = new TypeValidator();
  private final ToBuilderValidator.Factory toBuilderValidatorFactory;

  Analyser(Elements elements) {
    this.toBuilderValidatorFactory = new ToBuilderValidator.Factory(elements);
  }

  AnalysisResult parse(TypeElement buildElement) throws ValidationException {
    ClassName annotatedType = ClassName.get(buildElement);
    BuildConfig config = createBuildConfig(buildElement);
    ImmutableList.Builder<GoalContext> builder = ImmutableList.builder();
    for (ExecutableElement goal : goals(buildElement)) {
      TypeName goalType = goal.getKind() == CONSTRUCTOR
          ? annotatedType
          : TypeName.get(goal.getReturnType());
      typeValidator.validateBuildType(buildElement);
      ToBuilderValidator toBuilderValidator = toBuilderValidatorFactory
          .buildViaElement(goal).buildElement(buildElement);
      Goal goalAnnotation = goal.getAnnotation(Goal.class);
      boolean toBuilder = goalAnnotation != null && goalAnnotation.toBuilder();
      ImmutableList<ProjectionInfo> projectionInfos =
          toBuilder ? toBuilderValidator.validate() : toBuilderValidator.skip();
      builder.add(createGoalContext(goalType, config, projectionInfos, goal, toBuilder));
    }
    ImmutableList<GoalContext> goals = builder.build();
    checkMultipleToBuilder(goals);
    checkNameConflict(goals);
    return new AnalysisResult(config, goals);
  }
  private void checkMultipleToBuilder(ImmutableList<GoalContext> goals) throws ValidationException {
    ImmutableList<GoalContext> toBuilderGoals = FluentIterable.from(goals).filter(
        new Predicate<GoalContext>() {
          @Override
          public boolean apply(GoalContext goal) {
            return goal.innerContext.toBuilder;
          }
        }).toList();
    if (toBuilderGoals.size() > 1) {
      throw new ValidationException(MULTIPLE_TOBUILDER, toBuilderGoals.get(1).innerContext.goal);
    }
  }

  private void checkNameConflict(ImmutableList<GoalContext> goals) throws ValidationException {
    goals = ImmutableList.copyOf(CONSTRUCTORS_FIRST.sortedCopy(goals));
    HashMap<Object, ExecutableElement> goalNames = new HashMap<>();
    for (GoalContext goal : goals) {
      String goalName = goal.innerContext.goalName();
      ExecutableElement thisGoal = goal.innerContext.goal;
      ExecutableElement otherGoal = goalNames.put(goalName, thisGoal);
      if (otherGoal != null) {
        if (thisGoal.getKind() == CONSTRUCTOR
            && otherGoal.getKind() == CONSTRUCTOR
            && isNullOrEmpty(thisGoal.getAnnotation(Goal.class).name())
            && isNullOrEmpty(otherGoal.getAnnotation(Goal.class).name())) {
          throw new ValidationException("Multiple constructor goals found. Please add a goal name.",
              thisGoal);
        }
        if (thisGoal.getKind() == METHOD
            && otherGoal.getKind() == METHOD
            && isNullOrEmpty(thisGoal.getAnnotation(Goal.class).name())
            && isNullOrEmpty(otherGoal.getAnnotation(Goal.class).name())) {
          throw new ValidationException("Multiple goals with the same return type found. Please add a goal name.",
              thisGoal);
        }
        if (thisGoal.getKind() == METHOD
            && otherGoal.getKind() == CONSTRUCTOR
            && isNullOrEmpty(thisGoal.getAnnotation(Goal.class).name())
            && isNullOrEmpty(otherGoal.getAnnotation(Goal.class).name())) {
          throw new ValidationException("The return type is conflicting with a constructor goal. Please add a goal name.",
              thisGoal);
        }
        if (isNullOrEmpty(thisGoal.getAnnotation(Goal.class).name())) {
          throw new ValidationException("The return type is conflicting with another goal. Please add a goal name.",
              thisGoal);
        }
        throw new ValidationException("The goal name is conflicting with another goal. Please pick another one.",
            thisGoal);
      }
    }
  }

  private ImmutableList<ExecutableElement> goals(TypeElement buildElement) throws ValidationException {
    ImmutableList.Builder<ExecutableElement> builder = ImmutableList.builder();
    for (ExecutableElement executableElement : concat(constructorsIn(buildElement.getEnclosedElements()),
        methodsIn(buildElement.getEnclosedElements()))) {
      if (executableElement.getAnnotation(Goal.class) != null) {
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
