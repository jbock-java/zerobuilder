package net.zerobuilder.compiler;

import com.google.auto.common.MoreTypes;
import com.google.common.base.Function;
import com.google.common.base.Optional;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.Ordering;
import com.google.common.primitives.Ints;
import com.squareup.javapoet.ClassName;
import com.squareup.javapoet.CodeBlock;
import com.squareup.javapoet.TypeName;
import net.zerobuilder.Goal;
import net.zerobuilder.compiler.Analyser.AbstractGoalElement.GoalElementCases;
import net.zerobuilder.compiler.GoalContextFactory.GoalKind;
import net.zerobuilder.compiler.ToBuilderValidator.ValidParameter;

import javax.lang.model.element.Element;
import javax.lang.model.element.ElementKind;
import javax.lang.model.element.ExecutableElement;
import javax.lang.model.element.TypeElement;
import javax.lang.model.element.VariableElement;
import javax.lang.model.util.Elements;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;

import static com.google.auto.common.MoreElements.asExecutable;
import static com.google.auto.common.MoreElements.asVariable;
import static com.google.auto.common.MoreTypes.asTypeElement;
import static com.google.common.base.Strings.isNullOrEmpty;
import static com.google.common.collect.Iterables.getLast;
import static javax.lang.model.element.ElementKind.CONSTRUCTOR;
import static javax.lang.model.element.ElementKind.FIELD;
import static javax.lang.model.element.ElementKind.METHOD;
import static javax.lang.model.element.Modifier.PRIVATE;
import static javax.lang.model.element.Modifier.STATIC;
import static javax.tools.Diagnostic.Kind.WARNING;
import static net.zerobuilder.compiler.BuilderContext.createBuildConfig;
import static net.zerobuilder.compiler.GoalContextFactory.GoalKind.INSTANCE_METHOD;
import static net.zerobuilder.compiler.GoalContextFactory.GoalKind.STATIC_METHOD;
import static net.zerobuilder.compiler.GoalContextFactory.context;
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
import static net.zerobuilder.compiler.Utilities.downcase;
import static net.zerobuilder.compiler.Utilities.upcase;

final class Analyser {

  /**
   * to generate better error messages
   */
  private static final Ordering<GoalElement> GOAL_ORDER_FOR_DUPLICATE_NAME_CHECK
      = Ordering.from(new Comparator<GoalElement>() {

    private int goalWeight(GoalElement goal) {
      ElementKind kind = goal.element.getKind();
      Optional<Goal> annotation = goal.goalAnnotation;
      String name = annotation.transform(GOAL_NAME).or("");
      return isNullOrEmpty(name)
          ? (kind == CONSTRUCTOR ? 0 : (kind == METHOD ? 1 : 2))
          : (kind == CONSTRUCTOR ? 3 : (kind == METHOD ? 4 : 5));
    }

    @Override
    public int compare(GoalElement g0, GoalElement g1) {
      return Ints.compare(goalWeight(g0), goalWeight(g1));
    }
  });

  private final TypeValidator typeValidator = new TypeValidator();
  private final ToBuilderValidator.Factory toBuilderValidatorFactory;

  Analyser(Elements elements) {
    this.toBuilderValidatorFactory = new ToBuilderValidator.Factory(elements);
  }

  AnalysisResult parse(TypeElement buildElement) throws ValidationException {
    BuilderContext context = createBuildConfig(buildElement);
    ImmutableList.Builder<GoalContext> builder = ImmutableList.builder();
    ImmutableList<GoalElement> goals = goals(buildElement);
    checkNameConflict(goals);
    for (GoalElement goal : goals) {
      typeValidator.validateBuildType(buildElement);
      ToBuilderValidator toBuilderValidator = toBuilderValidatorFactory
          .goalElement(goal).buildElement(buildElement);
      boolean toBuilder = isToBuilder(goal);
      ImmutableList<ValidParameter> validParameters = toBuilder
          ? toBuilderValidator.validate()
          : toBuilderValidator.skip();
      CodeBlock goalCall = goalInvocation(goal, context.annotatedType);
      builder.add(context(goal, context, validParameters, toBuilder, goalCall));
    }
    return new AnalysisResult(context, builder.build());
  }

  private void checkNameConflict(ImmutableList<GoalElement> goals) throws ValidationException {
    goals = GOAL_ORDER_FOR_DUPLICATE_NAME_CHECK.immutableSortedCopy(goals);
    HashMap<String, GoalElement> goalNames = new HashMap<>();
    for (GoalElement goal : goals) {
      GoalElement otherGoal = goalNames.put(goal.name, goal);
      if (otherGoal != null) {
        Optional<Goal> goalAnnotation = goal.goalAnnotation;
        Optional<Goal> otherAnnotation = otherGoal.goalAnnotation;
        String thisName = goalAnnotation.transform(GOAL_NAME).or("");
        String otherName = otherAnnotation.transform(GOAL_NAME).or("");
        ElementKind thisKind = goal.element.getKind();
        ElementKind otherKind = otherGoal.element.getKind();
        if (isNullOrEmpty(thisName)) {
          if (thisKind == CONSTRUCTOR && otherKind == CONSTRUCTOR) {
            throw new ValidationException(GOALNAME_EECC, goal.element);
          }
          if (thisKind == METHOD && otherKind == CONSTRUCTOR) {
            throw new ValidationException(GOALNAME_EEMC, goal.element);
          }
          throw new ValidationException(GOALNAME_EEMM, goal.element);
        } else if (isNullOrEmpty(otherName)) {
          if (thisKind == CONSTRUCTOR && otherKind == CONSTRUCTOR) {
            throw new ValidationException(GOALNAME_NECC, goal.element);
          }
          if (thisKind == METHOD && otherKind == CONSTRUCTOR) {
            throw new ValidationException(GOALNAME_NEMC, goal.element);
          }
          throw new ValidationException(GOALNAME_NEMM, goal.element);
        }
        throw new ValidationException(GOALNAME_NN, goal.element);
      }
    }
  }

  private ImmutableList<GoalElement> goals(TypeElement buildElement) throws ValidationException {
    ImmutableList.Builder<GoalElement> builder = ImmutableList.builder();
    if (buildElement.getAnnotation(Goal.class) != null) {
      builder.add(FieldGoal.create(buildElement));
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
          builder.add(ExecutableGoal.create(executableElement));
        } else if (kind == FIELD) {
          VariableElement field = asVariable(element);
          builder.add(FieldGoal.create(field));
        }
      }
    }
    ImmutableList<GoalElement> goals = builder.build();
    if (goals.isEmpty()) {
      throw new ValidationException(WARNING, NO_GOALS, buildElement);
    }
    return goals;
  }

  static final class AnalysisResult {
    final BuilderContext config;
    final ImmutableList<GoalContext> goals;

    AnalysisResult(BuilderContext config, ImmutableList<GoalContext> goals) {
      this.config = config;
      this.goals = goals;
    }
  }

  private static String goalName(Goal goalAnnotation, TypeName goalType) {
    return isNullOrEmpty(goalAnnotation.name())
        ? ((ClassName) goalType.box()).simpleName()
        : upcase(goalAnnotation.name());
  }

  private static TypeName goalType(ExecutableElement goal) {
    switch (goal.getKind()) {
      case CONSTRUCTOR:
        return ClassName.get(goal.getEnclosingElement().asType());
      default:
        return TypeName.get(goal.getReturnType());
    }
  }

  private static CodeBlock goalInvocation(final GoalElement goal,
                                          final ClassName annotatedType) throws ValidationException {
    return goal.accept(new GoalElementCases<CodeBlock>() {
      @Override
      public CodeBlock executable(ExecutableElement element, GoalKind kind) throws ValidationException {
        CodeBlock parameters = goalParameters(element);
        String method = element.getSimpleName().toString();
        String returnLiteral = TypeName.VOID.equals(goal.goalType) ? "" : "return ";
        switch (kind) {
          case CONSTRUCTOR:
            return CodeBlock.builder()
                .addStatement("return new $T($L)", goal.goalType, parameters)
                .build();
          case INSTANCE_METHOD:
            String instance = downcase(annotatedType.simpleName());
            return CodeBlock.builder()
                .addStatement("$L$N.$N($L)", returnLiteral, "_" + instance, method, parameters)
                .build();
          case STATIC_METHOD:
            return CodeBlock.builder()
                .addStatement("$L$T.$N($L)", returnLiteral, annotatedType, method, parameters)
                .build();
          default:
            throw new IllegalStateException("unknown kind: " + kind);
        }
      }
      @Override
      public CodeBlock field(Element field, TypeElement typeElement) throws ValidationException {
        return CodeBlock.builder()
            .addStatement("return $L", downcase(((ClassName) goal.goalType).simpleName()))
            .build();
      }
    });
  }

  private static CodeBlock goalParameters(ExecutableElement element) throws ValidationException {
    CodeBlock.Builder builder = CodeBlock.builder();
    List<? extends VariableElement> parameters = element.getParameters();
    for (VariableElement arg : parameters.subList(0, parameters.size() - 1)) {
      builder.add(CodeBlock.of("$L, ", arg.getSimpleName()));
    }
    builder.add(CodeBlock.of("$L", getLast(parameters).getSimpleName()));
    return builder.build();
  }

  static abstract class AbstractGoalElement {
    interface GoalElementCases<R> {
      R executable(ExecutableElement element, GoalKind kind) throws ValidationException;
      R field(Element field, TypeElement typeElement) throws ValidationException;
    }
    abstract <R> R accept(GoalElementCases<R> goalElementCases) throws ValidationException;
  }

  static abstract class GoalElement extends AbstractGoalElement {
    final Element element;
    final Optional<Goal> goalAnnotation;
    final TypeName goalType;
    final String name;
    GoalElement(Element element, TypeName goalType, String name) {
      this.element = element;
      this.goalAnnotation = Optional.fromNullable(element.getAnnotation(Goal.class));
      this.goalType = goalType;
      this.name = name;
    }
  }

  static final class ExecutableGoal extends GoalElement {
    final GoalKind kind;
    final ExecutableElement executableElement;
    ExecutableGoal(ExecutableElement element, GoalKind kind, TypeName goalType, String name) {
      super(element, goalType, name);
      this.kind = kind;
      this.executableElement = element;
    }
    static GoalElement create(ExecutableElement element) {
      TypeName goalType = goalType(element);
      String name = goalName(element.getAnnotation(Goal.class), goalType);
      return new ExecutableGoal(element,
          element.getKind() == CONSTRUCTOR
              ? GoalKind.CONSTRUCTOR
              : element.getModifiers().contains(STATIC) ? STATIC_METHOD : INSTANCE_METHOD,
          goalType(element), name);

    }
    <R> R accept(GoalElementCases<R> goalElementCases) throws ValidationException {
      return goalElementCases.executable(executableElement, kind);
    }
  }

  static final class FieldGoal extends GoalElement {
    final Element field;
    final TypeElement typeElement;
    private FieldGoal(Element field, TypeName goalType, String name, TypeElement typeElement) {
      super(field, goalType, name);
      this.field = field;
      this.typeElement = typeElement;
    }
    private static GoalElement create(VariableElement field) {
      TypeName goalType = TypeName.get(field.asType());
      String name = goalName(field.getAnnotation(Goal.class), goalType);
      return new FieldGoal(field, goalType, name, asTypeElement(field.asType()));
    }
    private static GoalElement create(TypeElement field) {
      TypeName goalType = TypeName.get(field.asType());
      String name = goalName(field.getAnnotation(Goal.class), goalType);
      return new FieldGoal(field, goalType, name, field);
    }
    <R> R accept(GoalElementCases<R> goalElementCases) throws ValidationException {
      return goalElementCases.field(field, typeElement);
    }
  }

  static final Function<Goal, String> GOAL_NAME = new Function<Goal, String>() {
    @Override
    public String apply(Goal goal) {
      return goal.name();
    }
  };

  static final boolean isToBuilder(GoalElement goal) {
    Optional<Goal> goalAnnotation = goal.goalAnnotation;
    return goalAnnotation.isPresent() && goalAnnotation.get().toBuilder();
  }

}
