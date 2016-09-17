package net.zerobuilder.compiler;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.Ordering;
import com.google.common.primitives.Ints;
import com.squareup.javapoet.ClassName;
import com.squareup.javapoet.CodeBlock;
import com.squareup.javapoet.TypeName;
import net.zerobuilder.Goal;
import net.zerobuilder.compiler.Analyser.AbstractGoalElement.GoalElementCases;
import net.zerobuilder.compiler.GoalContextFactory.GoalKind;
import net.zerobuilder.compiler.ProjectionValidator.ValidationResult;

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
import static com.google.common.base.Preconditions.checkNotNull;
import static com.google.common.base.Strings.isNullOrEmpty;
import static com.google.common.base.Throwables.propagate;
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
import static net.zerobuilder.compiler.Messages.ErrorMessages.NOT_A_BEAN;
import static net.zerobuilder.compiler.Messages.ErrorMessages.NOT_ENOUGH_PARAMETERS;
import static net.zerobuilder.compiler.Messages.ErrorMessages.NO_GOALS;
import static net.zerobuilder.compiler.Messages.ErrorMessages.PRIVATE_METHOD;
import static net.zerobuilder.compiler.Utilities.downcase;

final class Analyser {

  /**
   * to generate better error messages, in case of goal name conflict
   */
  private static final Ordering<GoalElement> GOAL_ORDER_FOR_DUPLICATE_NAME_CHECK
      = Ordering.from(new Comparator<GoalElement>() {

    private int goalWeight(GoalElement goal) throws ValidationException {
      ElementKind kind = goal.accept(getElement).getKind();
      Goal annotation = goal.goalAnnotation;
      String name = annotation.name();
      return isNullOrEmpty(name)
          ? (kind == CONSTRUCTOR ? 0 : (kind == METHOD ? 1 : 2))
          : (kind == CONSTRUCTOR ? 3 : (kind == METHOD ? 4 : 5));
    }

    @Override
    public int compare(GoalElement g0, GoalElement g1) {
      try {
        return Ints.compare(goalWeight(g0), goalWeight(g1));
      } catch (ValidationException e) {
        propagate(e);
        return 0;
      }
    }
  });

  private final TypeValidator typeValidator = new TypeValidator();
  private final ProjectionValidator projectionValidator;

  Analyser(Elements elements) {
    this.projectionValidator = ProjectionValidator.create(elements);
  }

  AnalysisResult parse(TypeElement buildElement) throws ValidationException {
    BuilderContext context = createBuildConfig(buildElement);
    ImmutableList.Builder<GoalContext> builder = ImmutableList.builder();
    ImmutableList<GoalElement> goals = goals(buildElement);
    checkNameConflict(goals);
    for (GoalElement goal : goals) {
      typeValidator.validateBuildType(buildElement);
      boolean toBuilder = goal.goalAnnotation.toBuilder();
      boolean isBuilder = goal.goalAnnotation.builder();
      ValidationResult validationResult = toBuilder
          ? goal.accept(projectionValidator.validate)
          : goal.accept(ProjectionValidator.skip);
      CodeBlock goalCall = goalInvocation(goal, context.annotatedType);
      builder.add(context(validationResult, context, toBuilder, isBuilder, goalCall));
    }
    return new AnalysisResult(context, builder.build());
  }

  private void checkNameConflict(ImmutableList<GoalElement> goals) throws ValidationException {
    goals = GOAL_ORDER_FOR_DUPLICATE_NAME_CHECK.immutableSortedCopy(goals);
    HashMap<String, GoalElement> goalNames = new HashMap<>();
    for (GoalElement goal : goals) {
      GoalElement otherGoal = goalNames.put(goal.name, goal);
      if (otherGoal != null) {
        Goal goalAnnotation = goal.goalAnnotation;
        Goal otherAnnotation = otherGoal.goalAnnotation;
        String thisName = goalAnnotation.name();
        String otherName = otherAnnotation.name();
        ElementKind thisKind = goal.accept(getElement).getKind();
        ElementKind otherKind = otherGoal.accept(getElement).getKind();
        if (isNullOrEmpty(thisName)) {
          if (thisKind == CONSTRUCTOR && otherKind == CONSTRUCTOR) {
            throw new ValidationException(GOALNAME_EECC, goal.accept(getElement));
          }
          if (thisKind == METHOD && otherKind == CONSTRUCTOR) {
            throw new ValidationException(GOALNAME_EEMC, goal.accept(getElement));
          }
          throw new ValidationException(GOALNAME_EEMM, goal.accept(getElement));
        } else if (isNullOrEmpty(otherName)) {
          if (thisKind == CONSTRUCTOR && otherKind == CONSTRUCTOR) {
            throw new ValidationException(GOALNAME_NECC, goal.accept(getElement));
          }
          if (thisKind == METHOD && otherKind == CONSTRUCTOR) {
            throw new ValidationException(GOALNAME_NEMC, goal.accept(getElement));
          }
          throw new ValidationException(GOALNAME_NEMM, goal.accept(getElement));
        }
        throw new ValidationException(GOALNAME_NN, goal.accept(getElement));
      }
    }
  }

  private ImmutableList<GoalElement> goals(TypeElement buildElement) throws ValidationException {
    ImmutableList.Builder<GoalElement> builder = ImmutableList.builder();
    if (buildElement.getAnnotation(Goal.class) != null) {
      builder.add(BeanGoal.create(buildElement));
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
          builder.add(BeanGoal.create(field));
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
    String name = isNullOrEmpty(goalAnnotation.name())
        ? ((ClassName) goalType.box()).simpleName()
        : goalAnnotation.name();
    return downcase(name);
  }

  private static TypeName goalType(ExecutableElement goal) {
    switch (goal.getKind()) {
      case CONSTRUCTOR:
        return ClassName.get(goal.getEnclosingElement().asType());
      default:
        return TypeName.get(goal.getReturnType());
    }
  }

  private static CodeBlock goalInvocation(GoalElement goal,
                                          final ClassName annotatedType) throws ValidationException {
    return goal.accept(new GoalElementCases<CodeBlock>() {
      @Override
      public CodeBlock executable(ExecutableGoal goal) throws ValidationException {
        CodeBlock parameters = goalParameters(goal.executableElement);
        String method = goal.executableElement.getSimpleName().toString();
        String returnLiteral = TypeName.VOID.equals(goal.goalType) ? "" : "return ";
        switch (goal.kind) {
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
            throw new IllegalStateException("unknown kind: " + goal.kind);
        }
      }
      @Override
      public CodeBlock field(BeanGoal goal) throws ValidationException {
        return CodeBlock.builder()
            .addStatement("return $L", downcase(goal.goalType.simpleName()))
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
      R executable(ExecutableGoal executableGoal) throws ValidationException;
      R field(BeanGoal beanGoal) throws ValidationException;
    }
    abstract <R> R accept(GoalElementCases<R> goalElementCases) throws ValidationException;
  }

  static abstract class GoalElement extends AbstractGoalElement {
    final Goal goalAnnotation;
    final String name;
    GoalElement(Goal goalAnnotation, String name) {
      this.goalAnnotation = checkNotNull(goalAnnotation, "goalAnnotation");
      this.name = checkNotNull(name, "name");
    }
  }

  static final class ExecutableGoal extends GoalElement {
    final GoalKind kind;
    final TypeName goalType;
    final ExecutableElement executableElement;
    ExecutableGoal(ExecutableElement element, GoalKind kind, TypeName goalType, String name) {
      super(element.getAnnotation(Goal.class), name);
      this.goalType = goalType;
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
      return goalElementCases.executable(this);
    }
  }

  static final class BeanGoal extends GoalElement {
    final ClassName goalType;
    final TypeElement beanTypeElement;
    private BeanGoal(Element field, ClassName goalType, String name, TypeElement beanTypeElement) {
      super(field.getAnnotation(Goal.class), name);
      this.goalType = goalType;
      this.beanTypeElement = beanTypeElement;
    }
    private static GoalElement create(VariableElement field) throws ValidationException {
      TypeName typeName = ClassName.get(field.asType());
      if (!(typeName instanceof ClassName)) {
        throw new ValidationException(NOT_A_BEAN, field);
      }
      @SuppressWarnings("unchecked")
      ClassName goalType = (ClassName) typeName;
      String name = goalName(field.getAnnotation(Goal.class), goalType);
      return new BeanGoal(field, goalType, name, asTypeElement(field.asType()));
    }
    private static GoalElement create(TypeElement beanType) throws ValidationException {
      ClassName goalType = ClassName.get(beanType);
      String name = goalName(beanType.getAnnotation(Goal.class), goalType);
      return new BeanGoal(beanType, goalType, name, beanType);
    }
    <R> R accept(GoalElementCases<R> goalElementCases) throws ValidationException {
      return goalElementCases.field(this);
    }
  }

  static final GoalElementCases<Element> getElement = new GoalElementCases<Element>() {
    @Override
    public Element executable(ExecutableGoal executableGoal) throws ValidationException {
      return executableGoal.executableElement;
    }
    @Override
    public Element field(BeanGoal beanGoal) throws ValidationException {
      return beanGoal.beanTypeElement;
    }
  };
}
