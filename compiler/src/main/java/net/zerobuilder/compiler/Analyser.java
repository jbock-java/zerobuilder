package net.zerobuilder.compiler;

import com.google.common.base.Predicate;
import com.google.common.collect.FluentIterable;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.Ordering;
import com.google.common.primitives.Ints;
import com.squareup.javapoet.ClassName;
import com.squareup.javapoet.CodeBlock;
import com.squareup.javapoet.TypeName;
import net.zerobuilder.Goal;
import net.zerobuilder.compiler.ToBuilderValidator.ValidParameter;

import javax.lang.model.element.ElementKind;
import javax.lang.model.element.ExecutableElement;
import javax.lang.model.element.TypeElement;
import javax.lang.model.element.VariableElement;
import javax.lang.model.util.Elements;
import java.util.Comparator;
import java.util.HashMap;

import static com.google.common.base.Strings.isNullOrEmpty;
import static com.google.common.collect.Iterables.concat;
import static javax.lang.model.element.ElementKind.CONSTRUCTOR;
import static javax.lang.model.element.ElementKind.METHOD;
import static javax.lang.model.element.Modifier.PRIVATE;
import static javax.lang.model.util.ElementFilter.constructorsIn;
import static javax.lang.model.util.ElementFilter.methodsIn;
import static javax.tools.Diagnostic.Kind.WARNING;
import static net.zerobuilder.compiler.BuilderContext.createBuildConfig;
import static net.zerobuilder.compiler.Messages.ErrorMessages.GOALNAME_EECC;
import static net.zerobuilder.compiler.Messages.ErrorMessages.GOALNAME_EEMC;
import static net.zerobuilder.compiler.Messages.ErrorMessages.GOALNAME_EEMM;
import static net.zerobuilder.compiler.Messages.ErrorMessages.GOALNAME_NECC;
import static net.zerobuilder.compiler.Messages.ErrorMessages.GOALNAME_NEMC;
import static net.zerobuilder.compiler.Messages.ErrorMessages.GOALNAME_NEMM;
import static net.zerobuilder.compiler.Messages.ErrorMessages.GOALNAME_NN;
import static net.zerobuilder.compiler.Messages.ErrorMessages.MULTIPLE_TOBUILDER;
import static net.zerobuilder.compiler.Messages.ErrorMessages.NOT_ENOUGH_PARAMETERS;
import static net.zerobuilder.compiler.Messages.ErrorMessages.NO_GOALS;
import static net.zerobuilder.compiler.Messages.ErrorMessages.PRIVATE_METHOD;
import static net.zerobuilder.compiler.UberGoalContext.context;
import static net.zerobuilder.compiler.Utilities.joinCodeBlocks;
import static net.zerobuilder.compiler.Utilities.upcase;

final class Analyser {

  private static final Ordering<NamedGoal> CONSTRUCTORS_FIRST = Ordering.from(new Comparator<NamedGoal>() {

    private int goalWeight(NamedGoal goal) {
      ElementKind kind = goal.goal.getKind();
      Goal annotation = goal.goal.getAnnotation(Goal.class);
      String name = annotation == null ? "" : annotation.name();
      return isNullOrEmpty(name)
          ? kind == CONSTRUCTOR ? 0 : 1
          : kind == CONSTRUCTOR ? 2 : 3;
    }

    @Override
    public int compare(NamedGoal g0, NamedGoal g1) {
      return Ints.compare(goalWeight(g0), goalWeight(g1));
    }
  });

  private final TypeValidator typeValidator = new TypeValidator();
  private final ToBuilderValidator.Factory toBuilderValidatorFactory;

  Analyser(Elements elements) {
    this.toBuilderValidatorFactory = new ToBuilderValidator.Factory(elements);
  }

  AnalysisResult parse(TypeElement buildElement) throws ValidationException {
    BuilderContext config = createBuildConfig(buildElement);
    ImmutableList.Builder<UberGoalContext> builder = ImmutableList.builder();
    ImmutableList<NamedGoal> goals = goals(buildElement);
    checkMultipleToBuilder(goals);
    checkNameConflict(goals);
    for (NamedGoal goal : goals) {
      typeValidator.validateBuildType(buildElement);
      ToBuilderValidator toBuilderValidator = toBuilderValidatorFactory
          .buildViaElement(goal.goal).buildElement(buildElement);
      Goal goalAnnotation = goal.goal.getAnnotation(Goal.class);
      boolean toBuilder = goalAnnotation != null && goalAnnotation.toBuilder();
      ImmutableList<ValidParameter> validParameters =
          toBuilder ? toBuilderValidator.validate() : toBuilderValidator.skip();
      CodeBlock methodParameters = goalParameters(goal.goal);
      builder.add(context(goal.goalType, config, validParameters, goal.goal, toBuilder, methodParameters));
    }
    return new AnalysisResult(config, builder.build());
  }

  private void checkMultipleToBuilder(ImmutableList<NamedGoal> goals) throws ValidationException {
    ImmutableList<NamedGoal> toBuilderGoals = FluentIterable.from(goals).filter(
        new Predicate<NamedGoal>() {
          @Override
          public boolean apply(NamedGoal goal) {
            return goal.goal.getAnnotation(Goal.class).toBuilder();
          }
        }).toList();
    if (toBuilderGoals.size() > 1) {
      throw new ValidationException(MULTIPLE_TOBUILDER, toBuilderGoals.get(1).goal);
    }
  }

  private void checkNameConflict(ImmutableList<NamedGoal> goals) throws ValidationException {
    goals = ImmutableList.copyOf(CONSTRUCTORS_FIRST.sortedCopy(goals));
    HashMap<Object, ExecutableElement> goalNames = new HashMap<>();
    for (NamedGoal goal : goals) {
      ExecutableElement otherGoal = goalNames.put(goal.name, goal.goal);
      if (otherGoal != null) {
        String thisName = goal.goal.getAnnotation(Goal.class) == null ? ""
            : goal.goal.getAnnotation(Goal.class).name();
        String otherName = otherGoal.getAnnotation(Goal.class) == null ? ""
            : otherGoal.getAnnotation(Goal.class).name();
        ElementKind thisKind = goal.goal.getKind();
        ElementKind otherKind = otherGoal.getKind();
        if (isNullOrEmpty(thisName)) {
          if (thisKind == CONSTRUCTOR && otherKind == CONSTRUCTOR) {
            throw new ValidationException(GOALNAME_EECC, goal.goal);
          }
          if (thisKind == METHOD && otherKind == CONSTRUCTOR) {
            throw new ValidationException(GOALNAME_EEMC, goal.goal);
          }
          throw new ValidationException(GOALNAME_EEMM, goal.goal);
        } else if (isNullOrEmpty(otherName)) {
          if (thisKind == CONSTRUCTOR && otherKind == CONSTRUCTOR) {
            throw new ValidationException(GOALNAME_NECC, goal.goal);
          }
          if (thisKind == METHOD && otherKind == CONSTRUCTOR) {
            throw new ValidationException(GOALNAME_NEMC, goal.goal);
          }
          throw new ValidationException(GOALNAME_NEMM, goal.goal);
        }
        throw new ValidationException(GOALNAME_NN, goal.goal);
      }
    }
  }

  private ImmutableList<NamedGoal> goals(TypeElement buildElement) throws ValidationException {
    ImmutableList.Builder<NamedGoal> builder = ImmutableList.builder();
    for (ExecutableElement executableElement : concat(constructorsIn(buildElement.getEnclosedElements()),
        methodsIn(buildElement.getEnclosedElements()))) {
      if (executableElement.getAnnotation(Goal.class) != null) {
        if (executableElement.getModifiers().contains(PRIVATE)) {
          throw new ValidationException(PRIVATE_METHOD, buildElement);
        }
        if (executableElement.getParameters().isEmpty()) {
          throw new ValidationException(NOT_ENOUGH_PARAMETERS, buildElement);
        }
        TypeName goalType = goalType(executableElement);
        String name = goalName(goalType, executableElement);
        builder.add(new NamedGoal(name, executableElement, goalType));
      }
    }
    ImmutableList<NamedGoal> goals = builder.build();
    if (goals.isEmpty()) {
      throw new ValidationException(WARNING, NO_GOALS, buildElement);
    }
    return goals;
  }

  static final class AnalysisResult {
    final BuilderContext config;
    final ImmutableList<UberGoalContext> goals;

    AnalysisResult(BuilderContext config, ImmutableList<UberGoalContext> goals) {
      this.config = config;
      this.goals = goals;
    }
  }

  private static final class NamedGoal {

    private final String name;
    private final ExecutableElement goal;
    private final TypeName goalType;

    private NamedGoal(String name, ExecutableElement goal, TypeName goalType) {
      this.name = name;
      this.goal = goal;
      this.goalType = goalType;
    }

  }

  private static String goalName(TypeName goalType, ExecutableElement goal) {
    Goal goalAnnotation = goal.getAnnotation(Goal.class);
    if (goalAnnotation == null || isNullOrEmpty(goalAnnotation.name())) {
      return goalTypeName(goalType);
    }
    return upcase(goalAnnotation.name());
  }

  private static String goalTypeName(TypeName goalType) {
    return ((ClassName) goalType.box()).simpleName();
  }

  private static TypeName goalType(ExecutableElement goal) {
    switch (goal.getKind()) {
      case CONSTRUCTOR:
        return ClassName.get(goal.getEnclosingElement().asType());
      default:
        return TypeName.get(goal.getReturnType());
    }
  }

  private static CodeBlock goalParameters(ExecutableElement goal) {
    ImmutableList.Builder<CodeBlock> builder = ImmutableList.builder();
    for (VariableElement arg : goal.getParameters()) {
      builder.add(CodeBlock.of("$L", arg.getSimpleName()));
    }
    return joinCodeBlocks(builder.build(), ", ");
  }

}
