package net.zerobuilder.compiler.analyse;

import com.google.common.base.Function;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.Ordering;
import com.google.common.primitives.Ints;
import com.squareup.javapoet.ClassName;
import com.squareup.javapoet.CodeBlock;
import com.squareup.javapoet.TypeName;
import net.zerobuilder.Goal;
import net.zerobuilder.compiler.analyse.Analyser.AbstractGoalElement.GoalElementCases;
import net.zerobuilder.compiler.generate.BuilderType;
import net.zerobuilder.compiler.generate.GoalContext;
import net.zerobuilder.compiler.analyse.GoalContextFactory.GoalKind;
import net.zerobuilder.compiler.analyse.ProjectionValidator.ValidationResult;

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
import static com.google.common.base.Preconditions.checkNotNull;
import static com.google.common.base.Strings.isNullOrEmpty;
import static com.google.common.base.Throwables.propagate;
import static com.google.common.collect.Iterables.getLast;
import static javax.lang.model.element.ElementKind.CONSTRUCTOR;
import static javax.lang.model.element.ElementKind.METHOD;
import static javax.lang.model.element.Modifier.PRIVATE;
import static javax.lang.model.element.Modifier.STATIC;
import static javax.tools.Diagnostic.Kind.WARNING;
import static net.zerobuilder.compiler.generate.BuilderType.createBuilderContext;
import static net.zerobuilder.compiler.analyse.GoalContextFactory.GoalKind.INSTANCE_METHOD;
import static net.zerobuilder.compiler.analyse.GoalContextFactory.GoalKind.STATIC_METHOD;
import static net.zerobuilder.compiler.analyse.GoalContextFactory.context;
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
import static net.zerobuilder.compiler.analyse.TypeValidator.validateBuildersType;
import static net.zerobuilder.compiler.Utilities.downcase;

public final class Analyser {

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

  private final Elements elements;

  public Analyser(Elements elements) {
    this.elements = elements;
  }

  public AnalysisResult analyse(TypeElement buildElement) throws ValidationException {
    BuilderType context = createBuilderContext(buildElement);
    ImmutableList.Builder<GoalContext> builder = ImmutableList.builder();
    ImmutableList<GoalElement> goals = goals(buildElement);
    checkNameConflict(goals);
    for (GoalElement goal : goals) {
      validateBuildersType(buildElement);
      boolean toBuilder = goal.goalAnnotation.toBuilder();
      boolean isBuilder = goal.goalAnnotation.builder();
      ValidationResult validationResult = toBuilder
          ? goal.accept(ProjectionValidator.validate)
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
      builder.add(BeanGoal.create(buildElement, elements));
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
          builder.add(ExecutableGoal.create(executableElement, elements));
        }
      }
    }
    ImmutableList<GoalElement> goals = builder.build();
    if (goals.isEmpty()) {
      throw new ValidationException(WARNING, NO_GOALS, buildElement);
    }
    return goals;
  }

  public static final class AnalysisResult {
    public final BuilderType config;
    public final ImmutableList<GoalContext> goals;

    AnalysisResult(BuilderType config, ImmutableList<GoalContext> goals) {
      this.config = config;
      this.goals = goals;
    }
  }

  private static String goalName(Goal goalAnnotation, TypeName goalType) {
    return isNullOrEmpty(goalAnnotation.name())
        ? downcase(((ClassName) goalType.box()).simpleName())
        : goalAnnotation.name();
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
      public CodeBlock executableGoal(ExecutableGoal goal) throws ValidationException {
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
      public CodeBlock beanGoal(BeanGoal goal) throws ValidationException {
        return CodeBlock.builder()
            .addStatement("return $L", downcase(goal.goalType.simpleName()))
            .build();
      }
    });
  }

  private static CodeBlock goalParameters(ExecutableElement element) {
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
      R executableGoal(ExecutableGoal executableGoal);
      R beanGoal(BeanGoal beanGoal);
    }
    abstract <R> R accept(GoalElementCases<R> goalElementCases);
  }

  static <R> GoalElementCases<R> goalElementCases(
      final Function<ExecutableGoal, R> executableGoalFunction,
      final Function<BeanGoal, R> beanGoalFunction) {
    return new GoalElementCases<R>() {
      @Override
      public R executableGoal(ExecutableGoal executableGoal){
        return executableGoalFunction.apply(executableGoal);
      }
      @Override
      public R beanGoal(BeanGoal beanGoal) {
        return beanGoalFunction.apply(beanGoal);
      }
    };
  }

  static abstract class GoalElement extends AbstractGoalElement {
    final Goal goalAnnotation;
    final String name;
    final Elements elements;
    GoalElement(Goal goalAnnotation, String name, Elements elements) {
      this.goalAnnotation = checkNotNull(goalAnnotation, "goalAnnotation");
      this.name = checkNotNull(name, "name");
      this.elements = elements;
    }
  }

  static final class ExecutableGoal extends GoalElement {
    final GoalKind kind;
    final TypeName goalType;
    final ExecutableElement executableElement;
    ExecutableGoal(ExecutableElement element, GoalKind kind, TypeName goalType, String name,
                   Elements elements) {
      super(element.getAnnotation(Goal.class), name, elements);
      this.goalType = goalType;
      this.kind = kind;
      this.executableElement = element;
    }
    static GoalElement create(ExecutableElement element, Elements elements) {
      TypeName goalType = goalType(element);
      String name = goalName(element.getAnnotation(Goal.class), goalType);
      return new ExecutableGoal(element,
          element.getKind() == CONSTRUCTOR
              ? GoalKind.CONSTRUCTOR
              : element.getModifiers().contains(STATIC) ? STATIC_METHOD : INSTANCE_METHOD,
          goalType(element), name, elements);

    }
    <R> R accept(GoalElementCases<R> goalElementCases) {
      return goalElementCases.executableGoal(this);
    }
  }

  static final class BeanGoal extends GoalElement {
    final ClassName goalType;
    final TypeElement beanTypeElement;
    private BeanGoal(Element field, ClassName goalType, String name, TypeElement beanTypeElement, Elements elements) {
      super(field.getAnnotation(Goal.class), name, elements);
      this.goalType = goalType;
      this.beanTypeElement = beanTypeElement;
    }
    private static GoalElement create(TypeElement beanType, Elements elements) {
      ClassName goalType = ClassName.get(beanType);
      String name = goalName(beanType.getAnnotation(Goal.class), goalType);
      return new BeanGoal(beanType, goalType, name, beanType, elements);
    }
    <R> R accept(GoalElementCases<R> goalElementCases) {
      return goalElementCases.beanGoal(this);
    }
  }

  private static final GoalElementCases<Element> getElement = new GoalElementCases<Element>() {
    @Override
    public Element executableGoal(ExecutableGoal executableGoal) {
      return executableGoal.executableElement;
    }
    @Override
    public Element beanGoal(BeanGoal beanGoal) {
      return beanGoal.beanTypeElement;
    }
  };
}
