package net.zerobuilder.compiler;

import com.google.common.base.Function;
import com.google.common.base.Optional;
import com.google.common.base.Predicate;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.Ordering;
import com.google.common.primitives.Ints;
import com.squareup.javapoet.ClassName;
import com.squareup.javapoet.CodeBlock;
import com.squareup.javapoet.TypeName;
import net.zerobuilder.Goal;
import net.zerobuilder.compiler.ToBuilderValidator.ValidParameter;
import net.zerobuilder.compiler.UberGoalContext.GoalKind;

import javax.lang.model.element.Element;
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
import static javax.lang.model.element.Modifier.STATIC;
import static javax.lang.model.util.ElementFilter.constructorsIn;
import static javax.lang.model.util.ElementFilter.fieldsIn;
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
import static net.zerobuilder.compiler.Messages.ErrorMessages.NOT_ENOUGH_PARAMETERS;
import static net.zerobuilder.compiler.Messages.ErrorMessages.NO_GOALS;
import static net.zerobuilder.compiler.Messages.ErrorMessages.PRIVATE_METHOD;
import static net.zerobuilder.compiler.UberGoalContext.context;
import static net.zerobuilder.compiler.Utilities.joinCodeBlocks;
import static net.zerobuilder.compiler.Utilities.upcase;

final class Analyser {

  private static final Ordering<NamedGoal> CONSTRUCTORS_FIRST = Ordering.from(new Comparator<NamedGoal>() {

    private int goalWeight(NamedGoal goal) {
      ElementKind kind = goal.goal.element.getKind();
      Optional<Goal> annotation = goal.goal.goalAnnotation;
      String name = annotation.transform(GOAL_NAME).or("");
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
    checkNameConflict(goals);
    for (NamedGoal goal : goals) {
      typeValidator.validateBuildType(buildElement);
      ToBuilderValidator toBuilderValidator = toBuilderValidatorFactory
          .goalElement(goal.goal).buildElement(buildElement);
      boolean toBuilder = GOAL_TOBUILDER.apply(goal.goal);
      ImmutableList<ValidParameter> validParameters =
          toBuilder ? toBuilderValidator.validate() : toBuilderValidator.skip();
      CodeBlock methodParameters = goalParameters(goal.goal);
      builder.add(context(goal.goalType, config, validParameters, goal.goal, toBuilder, methodParameters));
    }
    return new AnalysisResult(config, builder.build());
  }

  private void checkNameConflict(ImmutableList<NamedGoal> goals) throws ValidationException {
    goals = ImmutableList.copyOf(CONSTRUCTORS_FIRST.sortedCopy(goals));
    HashMap<Object, NamedGoal> goalNames = new HashMap<>();
    for (NamedGoal goal : goals) {
      NamedGoal otherGoal = goalNames.put(goal.name, goal);
      if (otherGoal != null) {
        String thisName = goal.name;
        String otherName = otherGoal.name;
        ElementKind thisKind = goal.goal.element.getKind();
        ElementKind otherKind = otherGoal.goal.element.getKind();
        if (isNullOrEmpty(thisName)) {
          if (thisKind == CONSTRUCTOR && otherKind == CONSTRUCTOR) {
            throw new ValidationException(GOALNAME_EECC, goal.goal.element);
          }
          if (thisKind == METHOD && otherKind == CONSTRUCTOR) {
            throw new ValidationException(GOALNAME_EEMC, goal.goal.element);
          }
          throw new ValidationException(GOALNAME_EEMM, goal.goal.element);
        } else if (isNullOrEmpty(otherName)) {
          if (thisKind == CONSTRUCTOR && otherKind == CONSTRUCTOR) {
            throw new ValidationException(GOALNAME_NECC, goal.goal.element);
          }
          if (thisKind == METHOD && otherKind == CONSTRUCTOR) {
            throw new ValidationException(GOALNAME_NEMC, goal.goal.element);
          }
          throw new ValidationException(GOALNAME_NEMM, goal.goal.element);
        }
        throw new ValidationException(GOALNAME_NN, goal.goal.element);
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
        builder.add(new NamedGoal(name, new ExecutableGoal(executableElement), goalType));
      }
    }
    for (VariableElement field : fieldsIn(buildElement.getEnclosedElements())) {
      throw new IllegalStateException("todo");
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
    private final GoalElement goal;
    private final TypeName goalType;

    private NamedGoal(String name, GoalElement goal, TypeName goalType) {
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

  private static CodeBlock goalParameters(GoalElement goal) {
    ImmutableList.Builder<CodeBlock> builder = ImmutableList.builder();
    for (VariableElement arg : goal.getParameters()) {
      builder.add(CodeBlock.of("$L", arg.getSimpleName()));
    }
    return joinCodeBlocks(builder.build(), ", ");
  }

  static abstract class AbstractGoalElement {
    interface Cases<R extends Element> {
      R executable(ExecutableElement element);
      R field(VariableElement field);
    }
    abstract <R extends Element> R accept(Cases<R> cases);
  }

  static abstract class GoalElement<E extends Element> extends AbstractGoalElement {
    final E element;
    Optional<Goal> goalAnnotation;
    GoalElement(E element) {
      this.element = element;
      this.goalAnnotation = Optional.fromNullable(element.getAnnotation(Goal.class));
    }
    abstract GoalKind goalKind();

  }

  static final class ExecutableGoal extends GoalElement<ExecutableElement> {
    ExecutableGoal(ExecutableElement element) {
      super(element);
    }
    GoalKind goalKind() {
      return element.getKind() == CONSTRUCTOR
          ? GoalKind.CONSTRUCTOR
          : element.getModifiers().contains(STATIC)
          ? GoalKind.STATIC_METHOD
          : GoalKind.INSTANCE_METHOD;

    }
    <R extends Element> R accept(Cases<R> cases) {
      return cases.executable(element);
    }
  }

  static final class FieldGoal extends GoalElement<VariableElement> {
    FieldGoal(VariableElement field) {
      super(field);
    }
    @Override
    GoalKind goalKind() {
      return GoalKind.FIELD;
    }
    <R extends Element> R accept(Cases<R> cases) {
      return cases.field(element);
    }
  }

  static final Function<Goal, String> GOAL_NAME = new Function<Goal, String>() {
    @Override
    public String apply(Goal goal) {
      return goal.name();
    }
  };

  static final Function<NamedGoal, GoalElement> GET_GOAL = new Function<NamedGoal, GoalElement>() {
    @Override
    public GoalElement apply(NamedGoal namedGoal) {
      return namedGoal.goal;
    }
  }

  static final Predicate<GoalElement> GOAL_TOBUILDER = new Predicate<GoalElement>() {
    @Override
    public boolean apply(GoalElement goal) {
      Optional<Goal> goalAnnotation = goal.goalAnnotation;
      return goalAnnotation.isPresent() && goalAnnotation.get().toBuilder();
    }
  };

}
