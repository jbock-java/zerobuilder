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
import static net.zerobuilder.compiler.BuildConfig.createBuildConfig;
import static net.zerobuilder.compiler.UberGoalContext.createGoalContext;
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
    ClassName annotatedType = ClassName.get(buildElement);
    BuildConfig config = createBuildConfig(buildElement);
    ImmutableList.Builder<UberGoalContext> builder = ImmutableList.builder();
    ImmutableList<NamedGoal> goals = goals(annotatedType, buildElement);
    checkMultipleToBuilder(goals);
    checkNameConflict(goals);
    for (NamedGoal goal : goals) {
      TypeName goalType = goal.goal.getKind() == CONSTRUCTOR
          ? annotatedType
          : TypeName.get(goal.goal.getReturnType());
      typeValidator.validateBuildType(buildElement);
      ToBuilderValidator toBuilderValidator = toBuilderValidatorFactory
          .buildViaElement(goal.goal).buildElement(buildElement);
      Goal goalAnnotation = goal.goal.getAnnotation(Goal.class);
      boolean toBuilder = goalAnnotation != null && goalAnnotation.toBuilder();
      ImmutableList<ValidParameter> validParameters =
          toBuilder ? toBuilderValidator.validate() : toBuilderValidator.skip();
      builder.add(createGoalContext(goalType, config, validParameters, goal.goal, toBuilder, goalParameters(goal.goal)));
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
      String goalName = goal.name;
      ExecutableElement thisGoal = goal.goal;
      ExecutableElement otherGoal = goalNames.put(goalName, thisGoal);
      if (otherGoal != null) {
        String thisName = thisGoal.getAnnotation(Goal.class) == null ? ""
            : thisGoal.getAnnotation(Goal.class).name();
        String otherName = otherGoal.getAnnotation(Goal.class) == null ? ""
            : otherGoal.getAnnotation(Goal.class).name();
        ElementKind thisKind = thisGoal.getKind();
        ElementKind otherKind = otherGoal.getKind();
        if (isNullOrEmpty(thisName)) {
          if (thisKind == CONSTRUCTOR && otherKind == CONSTRUCTOR) {
            throw new ValidationException(GOALNAME_EECC, thisGoal);
          }
          if (thisKind == METHOD && otherKind == CONSTRUCTOR) {
            throw new ValidationException(GOALNAME_EEMC, thisGoal);
          }
          throw new ValidationException(GOALNAME_EEMM, thisGoal);
        } else if (isNullOrEmpty(otherName)) {
          if (thisKind == CONSTRUCTOR && otherKind == CONSTRUCTOR) {
            throw new ValidationException(GOALNAME_NECC, thisGoal);
          }
          if (thisKind == METHOD && otherKind == CONSTRUCTOR) {
            throw new ValidationException(GOALNAME_NEMC, thisGoal);
          }
          throw new ValidationException(GOALNAME_NEMM, thisGoal);
        }
        throw new ValidationException(GOALNAME_NN, thisGoal);
      }
    }
  }

  private ImmutableList<NamedGoal> goals(ClassName annotatedType, TypeElement buildElement) throws ValidationException {
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
        TypeName goalType = goalType(annotatedType, executableElement);
        builder.add(new NamedGoal(goalName(goalType, executableElement), executableElement));
      }
    }
    ImmutableList<NamedGoal> goals = builder.build();
    if (goals.isEmpty()) {
      throw new ValidationException(WARNING, NO_GOALS, buildElement);
    }
    return goals;
  }

  static final class AnalysisResult {
    final BuildConfig config;
    final ImmutableList<UberGoalContext> goals;

    AnalysisResult(BuildConfig config, ImmutableList<UberGoalContext> goals) {
      this.config = config;
      this.goals = goals;
    }
  }

  private static final class NamedGoal {

    private final String name;
    private final ExecutableElement goal;

    private NamedGoal(String name, ExecutableElement goal) {
      this.name = name;
      this.goal = goal;
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

  private static TypeName goalType(ClassName annotatedType, ExecutableElement goal) {
    return goal.getKind() == CONSTRUCTOR
        ? annotatedType
        : TypeName.get(goal.getReturnType());
  }

  private static CodeBlock goalParameters(ExecutableElement goal) {
    ImmutableList.Builder<CodeBlock> builder = ImmutableList.builder();
    for (VariableElement arg : goal.getParameters()) {
      builder.add(CodeBlock.of("$L", arg.getSimpleName()));
    }
    return joinCodeBlocks(builder.build(), ", ");
  }

}
