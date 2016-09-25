package net.zerobuilder.compiler.analyse;

import com.google.common.collect.ImmutableList;
import com.squareup.javapoet.CodeBlock;
import net.zerobuilder.Goal;
import net.zerobuilder.compiler.analyse.DtoPackage.GoalTypes.AbstractGoalElement;
import net.zerobuilder.compiler.analyse.DtoPackage.GoalTypes.BeanGoalElement;
import net.zerobuilder.compiler.analyse.DtoPackage.GoalTypes.GoalElementCases;
import net.zerobuilder.compiler.analyse.DtoPackage.GoalTypes.RegularGoalElement;
import net.zerobuilder.compiler.analyse.DtoShared.AnalysisResult;
import net.zerobuilder.compiler.analyse.DtoShared.ValidGoal;
import net.zerobuilder.compiler.generate.BuildersType;
import net.zerobuilder.compiler.generate.GoalContext.AbstractContext;

import javax.lang.model.element.Element;
import javax.lang.model.element.ElementKind;
import javax.lang.model.element.ExecutableElement;
import javax.lang.model.element.TypeElement;
import javax.lang.model.element.VariableElement;
import javax.lang.model.util.Elements;
import java.util.List;

import static com.google.auto.common.MoreElements.asExecutable;
import static com.google.common.collect.Iterables.getLast;
import static com.squareup.javapoet.TypeName.VOID;
import static javax.lang.model.element.ElementKind.CONSTRUCTOR;
import static javax.lang.model.element.ElementKind.METHOD;
import static javax.lang.model.element.Modifier.PRIVATE;
import static javax.tools.Diagnostic.Kind.WARNING;
import static net.zerobuilder.compiler.Messages.ErrorMessages.NOT_ENOUGH_PARAMETERS;
import static net.zerobuilder.compiler.Messages.ErrorMessages.NO_GOALS;
import static net.zerobuilder.compiler.Messages.ErrorMessages.PRIVATE_METHOD;
import static net.zerobuilder.compiler.Utilities.downcase;
import static net.zerobuilder.compiler.analyse.GoalContextFactory.context;
import static net.zerobuilder.compiler.analyse.GoalnameValidator.checkNameConflict;
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
      ValidGoal validGoal = toBuilder
          ? goal.accept(ProjectionValidator.validate)
          : goal.accept(ProjectionValidator.skip);
      CodeBlock goalCall = goalInvocation(goal, context);
      builder.add(context(validGoal, context, toBuilder, isBuilder, goalCall));
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

  private static CodeBlock goalInvocation(AbstractGoalElement goal,
                                          final BuildersType builders) throws ValidationException {
    return goal.accept(new GoalElementCases<CodeBlock>() {
      @Override
      public CodeBlock regularGoal(RegularGoalElement goal) throws ValidationException {
        CodeBlock parameters = goalParameters(goal.executableElement);
        String method = goal.executableElement.getSimpleName().toString();
        CodeBlock.Builder builder = CodeBlock.builder();
        builder.add(VOID.equals(goal.goal.goalType) ?
            CodeBlock.of("") :
            CodeBlock.of("return "));
        switch (goal.goal.kind) {
          case CONSTRUCTOR:
            return builder
                .addStatement("new $T($L)",
                    goal.goal.goalType, parameters).build();
          case INSTANCE_METHOD:
            return builder.addStatement("$N.$N($L)",
                builders.field, method, parameters).build();
          case STATIC_METHOD:
            return builder.addStatement("$T.$N($L)",
                builders.type, method, parameters).build();
          default:
            throw new IllegalStateException("unknown kind: " + goal.goal.kind);
        }
      }
      @Override
      public CodeBlock beanGoal(BeanGoalElement goal) throws ValidationException {
        return CodeBlock.builder()
            .addStatement("return $L", downcase(goal.goal.goalType.simpleName()))
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
}
