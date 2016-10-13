package net.zerobuilder.compiler.generate;

import com.squareup.javapoet.ClassName;
import com.squareup.javapoet.FieldSpec;
import com.squareup.javapoet.TypeSpec;
import net.zerobuilder.compiler.generate.DtoContext.BuildersContext;
import net.zerobuilder.compiler.generate.DtoGeneratorOutput.BuilderMethod;
import net.zerobuilder.compiler.generate.DtoGeneratorOutput.GeneratorOutput;
import net.zerobuilder.compiler.generate.DtoGoal.AbstractGoalDetails;
import net.zerobuilder.compiler.generate.DtoGoalContext.AbstractGoalContext;
import net.zerobuilder.compiler.generate.DtoGoalContext.IGoal;
import net.zerobuilder.compiler.generate.DtoGoalDescription.GoalDescription;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Function;
import java.util.stream.Stream;

import static java.util.Collections.emptyList;
import static java.util.stream.Collectors.toList;
import static javax.lang.model.element.Modifier.FINAL;
import static javax.lang.model.element.Modifier.PRIVATE;
import static net.zerobuilder.compiler.generate.Builder.defineBuilderImpl;
import static net.zerobuilder.compiler.generate.Builder.defineContract;
import static net.zerobuilder.compiler.generate.DtoContext.BuilderLifecycle.REUSE_INSTANCES;
import static net.zerobuilder.compiler.generate.DtoGoalContext.abstractGoalDetails;
import static net.zerobuilder.compiler.generate.DtoGoalContext.builder;
import static net.zerobuilder.compiler.generate.DtoGoalContext.builderImplType;
import static net.zerobuilder.compiler.generate.DtoGoalContext.goalCases;
import static net.zerobuilder.compiler.generate.DtoGoalContext.goalName;
import static net.zerobuilder.compiler.generate.DtoGoalContext.toBuilder;
import static net.zerobuilder.compiler.generate.GeneratorB.goalToBuilderB;
import static net.zerobuilder.compiler.generate.GeneratorB.goalToUpdaterB;
import static net.zerobuilder.compiler.generate.GeneratorV.goalToBuilderV;
import static net.zerobuilder.compiler.generate.GeneratorV.goalToUpdaterV;
import static net.zerobuilder.compiler.generate.GoalContextFactory.prepareGoal;
import static net.zerobuilder.compiler.generate.Updater.defineUpdater;
import static net.zerobuilder.compiler.generate.Updater.updaterType;
import static net.zerobuilder.compiler.generate.Utilities.downcase;
import static net.zerobuilder.compiler.generate.Utilities.transform;

public final class Generator {

  /**
   * Entry point for code generation.
   *
   * @param goals Goal descriptions
   * @return a GeneratorOutput
   */
  public static GeneratorOutput generate(GeneratorInput goals) {
    Function<GoalDescription, IGoal> prepare = prepareGoal(goals.buildersContext.generatedType);
    List<IGoal> iGoals = transform(goals.goals, prepare);
    return generate(goals.buildersContext, iGoals);
  }

  private static GeneratorOutput generate(BuildersContext context, List<IGoal> iGoals) {
    return new GeneratorOutput(
        methods(goals(context, iGoals)),
        nestedTypes(goals(context, iGoals)),
        fields(context, goals(context, iGoals)),
        context.generatedType,
        context.lifecycle);
  }

  private static List<FieldSpec> fields(BuildersContext context, List<AbstractGoalContext> goals) {
    return context.lifecycle == REUSE_INSTANCES ?
        Stream.concat(
            Stream.of(context.cache.get()),
            instanceFields(goals).stream())
            .collect(toList()) :
        emptyList();
  }

  private static List<BuilderMethod> methods(List<AbstractGoalContext> goals) {
    return Stream.concat(
        goals.stream()
            .filter(builder)
            .map(goalToBuilder),
        goals.stream()
            .filter(toBuilder)
            .map(goalToUpdater))
        .collect(toList());
  }

  private static List<TypeSpec> nestedTypes(List<AbstractGoalContext> goals) {
    List<TypeSpec> builder = new ArrayList<>();
    for (AbstractGoalContext goal : goals) {
      AbstractGoalDetails details = abstractGoalDetails.apply(goal);
      if (details.goalOptions.toBuilder) {
        builder.add(defineUpdater(goal));
      }
      if (details.goalOptions.builder) {
        builder.add(defineBuilderImpl(goal));
        builder.add(defineContract(goal));
      }
    }
    return builder;
  }

  private static List<FieldSpec> instanceFields(List<AbstractGoalContext> goals) {
    List<FieldSpec> builder = new ArrayList<>();
    for (AbstractGoalContext goal : goals) {
      AbstractGoalDetails details = abstractGoalDetails.apply(goal);
      if (details.goalOptions.toBuilder) {
        builder.add(updaterField(goal));
      }
      if (details.goalOptions.builder) {
        builder.add(builderField(goal));
      }
    }
    return builder;
  }

  private static final Function<AbstractGoalContext, BuilderMethod> goalToUpdater
      = goalCases(goalToUpdaterV, goalToUpdaterB);

  private static final Function<AbstractGoalContext, BuilderMethod> goalToBuilder
      = goalCases(goalToBuilderV, goalToBuilderB);

  private static List<AbstractGoalContext> goals(BuildersContext context, List<IGoal> goals) {
    return transform(goals, goal -> goal.withContext(context));
  }

  static FieldSpec updaterField(AbstractGoalContext goal) {
    ClassName type = updaterType(goal);
    return FieldSpec.builder(type, downcase(goalName.apply(goal) + "Updater"), PRIVATE, FINAL)
        .initializer("new $T()", type)
        .build();
  }

  static FieldSpec builderField(AbstractGoalContext goal) {
    ClassName type = builderImplType(goal);
    return FieldSpec.builder(type, downcase(goalName.apply(goal) + "BuilderImpl"), PRIVATE, FINAL)
        .initializer("new $T()", type)
        .build();
  }
}
