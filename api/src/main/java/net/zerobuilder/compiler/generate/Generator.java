package net.zerobuilder.compiler.generate;

import com.squareup.javapoet.ClassName;
import com.squareup.javapoet.FieldSpec;
import com.squareup.javapoet.TypeSpec;
import net.zerobuilder.compiler.generate.DtoGeneratorOutput.BuilderMethod;
import net.zerobuilder.compiler.generate.DtoGeneratorOutput.GeneratorOutput;
import net.zerobuilder.compiler.generate.DtoGoal.AbstractGoalDetails;
import net.zerobuilder.compiler.generate.DtoGoalContext.AbstractGoalContext;
import net.zerobuilder.compiler.generate.DtoGoalContext.IGoal;
import net.zerobuilder.compiler.generate.DtoGoalDescription.GoalDescription;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Function;

import static java.util.Collections.emptyList;
import static java.util.stream.Collectors.toList;
import static javax.lang.model.element.Modifier.FINAL;
import static javax.lang.model.element.Modifier.PRIVATE;
import static net.zerobuilder.compiler.generate.Builder.defineBuilderImpl;
import static net.zerobuilder.compiler.generate.Builder.defineContract;
import static net.zerobuilder.compiler.generate.DtoBuildersContext.BuilderLifecycle.NEW_INSTANCE;
import static net.zerobuilder.compiler.generate.DtoGoalContext.abstractGoalDetails;
import static net.zerobuilder.compiler.generate.DtoGoalContext.builderImplType;
import static net.zerobuilder.compiler.generate.DtoGoalContext.goalCases;
import static net.zerobuilder.compiler.generate.DtoGoalContext.goalName;
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
    List<IGoal> builder = transform(goals.validGoals, prepare);
    return generate(new Goals(goals.buildersContext, builder));
  }

  private static GeneratorOutput generate(Goals analysisResult) {
    List<AbstractGoalContext> goals = goals(analysisResult);
    List<BuilderMethod> methods = new ArrayList<>();
    methods.addAll(builderMethods(goals));
    methods.addAll(toBuilderMethods(goals));
    List<FieldSpec> fields = new ArrayList<>();
    if (analysisResult.buildersContext.lifecycle.recycle()) {
      fields.add(analysisResult.buildersContext.cache);
    }
    fields.addAll(instanceFields(analysisResult, goals));
    return new GeneratorOutput(methods,
        nestedGoalTypes(goals), fields, analysisResult.buildersContext.generatedType);
  }

  private static List<TypeSpec> nestedGoalTypes(List<AbstractGoalContext> goals) {
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

  private static List<BuilderMethod> toBuilderMethods(List<AbstractGoalContext> goals) {
    return goals.stream()
        .filter(goal -> {
          AbstractGoalDetails details = abstractGoalDetails.apply(goal);
          return details.goalOptions.toBuilder;
        })
        .map(goalToToBuilder)
        .collect(toList());
  }

  private static List<BuilderMethod> builderMethods(List<AbstractGoalContext> goals) {
    return goals.stream()
        .filter(goal -> {
          AbstractGoalDetails details = abstractGoalDetails.apply(goal);
          return details.goalOptions.builder;
        })
        .map(goalToBuilder)
        .collect(toList());
  }

  private static List<FieldSpec> instanceFields(Goals analysisResult,
                                                List<AbstractGoalContext> goals) {
    if (analysisResult.buildersContext.lifecycle == NEW_INSTANCE) {
      return emptyList();
    }
    List<FieldSpec> builder = new ArrayList<>();
    for (AbstractGoalContext goal : goals) {
      AbstractGoalDetails details = abstractGoalDetails.apply(goal);
      if (details.goalOptions.toBuilder) {
        builder.add(updaterField(goal));
      }
      if (details.goalOptions.builder) {
        builder.add(stepsField(goal));
      }
    }
    return builder;
  }

  private static final Function<AbstractGoalContext, BuilderMethod> goalToToBuilder
      = goalCases(GeneratorV.goalToToBuilder, GeneratorB.goalToToBuilder);

  private static final Function<AbstractGoalContext, BuilderMethod> goalToBuilder
      = goalCases(GeneratorV.goalToBuilder, GeneratorB.goalToBuilder);

  private static List<AbstractGoalContext> goals(final Goals goals) {
    return transform(goals.goals, goal -> goal.withContext(goals.buildersContext));
  }

  static FieldSpec updaterField(AbstractGoalContext goal) {
    ClassName type = updaterType(goal);
    return FieldSpec.builder(type, downcase(goalName.apply(goal) + "Updater"), PRIVATE, FINAL)
        .initializer("new $T()", type)
        .build();
  }

  static FieldSpec stepsField(AbstractGoalContext goal) {
    ClassName type = builderImplType(goal);
    return FieldSpec.builder(type, downcase(goalName.apply(goal) + "BuilderImpl"), PRIVATE, FINAL)
        .initializer("new $T()", type)
        .build();
  }

  private static final class Goals {
    private final DtoBuildersContext.BuildersContext buildersContext;
    private final List<? extends IGoal> goals;

    private Goals(DtoBuildersContext.BuildersContext buildersContext,
                  List<? extends IGoal> goals) {
      this.buildersContext = buildersContext;
      this.goals = goals;
    }
  }
}
