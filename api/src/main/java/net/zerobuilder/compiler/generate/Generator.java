package net.zerobuilder.compiler.generate;

import com.google.common.base.Function;
import com.google.common.base.Predicate;
import com.google.common.collect.FluentIterable;
import com.google.common.collect.ImmutableList;
import com.squareup.javapoet.ClassName;
import com.squareup.javapoet.FieldSpec;
import com.squareup.javapoet.TypeSpec;
import net.zerobuilder.compiler.generate.DtoGeneratorOutput.BuilderMethod;
import net.zerobuilder.compiler.generate.DtoGeneratorOutput.GeneratorOutput;
import net.zerobuilder.compiler.generate.DtoGoal.AbstractGoalDetails;
import net.zerobuilder.compiler.generate.DtoGoalContext.AbstractGoalContext;
import net.zerobuilder.compiler.generate.DtoGoalContext.IGoal;
import net.zerobuilder.compiler.generate.DtoGoalDescription.GoalDescription;

import java.util.List;

import static javax.lang.model.element.Modifier.FINAL;
import static javax.lang.model.element.Modifier.PRIVATE;
import static net.zerobuilder.compiler.generate.BuilderContext.defineBuilderImpl;
import static net.zerobuilder.compiler.generate.BuilderContext.defineContract;
import static net.zerobuilder.compiler.generate.DtoGoalContext.abstractGoalDetails;
import static net.zerobuilder.compiler.generate.DtoGoalContext.builderImplType;
import static net.zerobuilder.compiler.generate.DtoGoalContext.goalCases;
import static net.zerobuilder.compiler.generate.DtoGoalContext.goalName;
import static net.zerobuilder.compiler.generate.GoalContextFactory.prepareGoal;
import static net.zerobuilder.compiler.generate.UpdaterContext.defineUpdater;
import static net.zerobuilder.compiler.generate.UpdaterContext.updaterType;
import static net.zerobuilder.compiler.generate.Utilities.downcase;

public final class Generator {

  /**
   * Entry point for code generation.
   *
   * @param goals Goal descriptions
   * @return a GeneratorOutput
   */
  public static GeneratorOutput generate(GeneratorInput goals) {
    Function<GoalDescription, IGoal> prepare = prepareGoal(goals.buildersContext.generatedType);
    ImmutableList.Builder<IGoal> builder = ImmutableList.builder();
    for (GoalDescription goalDescription : goals.validGoals) {
      builder.add(prepare.apply(goalDescription));
    }
    return generate(new Goals(goals.buildersContext, builder.build()));
  }

  private static DtoGeneratorOutput.GeneratorSuccess generate(Goals analysisResult) {
    ImmutableList<AbstractGoalContext> goals = goals(analysisResult);
    ImmutableList.Builder<BuilderMethod> methods = ImmutableList.builder();
    methods.addAll(builderMethods(goals));
    methods.addAll(toBuilderMethods(goals));
    ImmutableList.Builder<FieldSpec> fields = ImmutableList.builder();
    if (analysisResult.buildersContext.recycle) {
      fields.add(analysisResult.buildersContext.cache);
    }
    fields.addAll(instanceFields(analysisResult, goals));
    return new DtoGeneratorOutput.GeneratorSuccess(methods.build(),
        nestedGoalTypes(goals), fields.build(), analysisResult.buildersContext.generatedType);
  }

  private static ImmutableList<TypeSpec> nestedGoalTypes(ImmutableList<AbstractGoalContext> goals) {
    ImmutableList.Builder<TypeSpec> builder = ImmutableList.builder();
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
    return builder.build();
  }

  private static ImmutableList<BuilderMethod> toBuilderMethods(ImmutableList<AbstractGoalContext> goals) {
    return FluentIterable.from(goals)
        .filter(new Predicate<AbstractGoalContext>() {
          @Override
          public boolean apply(AbstractGoalContext goal) {
            AbstractGoalDetails details = abstractGoalDetails.apply(goal);
            return details.goalOptions.toBuilder;
          }
        })
        .transform(goalToToBuilder)
        .toList();
  }

  private static ImmutableList<BuilderMethod> builderMethods(ImmutableList<AbstractGoalContext> goals) {
    return FluentIterable.from(goals)
        .filter(new Predicate<AbstractGoalContext>() {
          @Override
          public boolean apply(AbstractGoalContext goal) {
            AbstractGoalDetails details = abstractGoalDetails.apply(goal);
            return details.goalOptions.builder;
          }
        })
        .transform(goalToBuilder)
        .toList();
  }

  private static ImmutableList<FieldSpec> instanceFields(Goals analysisResult,
                                                         ImmutableList<AbstractGoalContext> goals) {
    if (!analysisResult.buildersContext.recycle) {
      return ImmutableList.of();
    }
    ImmutableList.Builder<FieldSpec> builder = ImmutableList.builder();
    for (AbstractGoalContext goal : goals) {
      AbstractGoalDetails details = abstractGoalDetails.apply(goal);
      if (details.goalOptions.toBuilder) {
        ClassName updaterType = updaterType(goal);
        builder.add(FieldSpec.builder(updaterType,
            updaterField(goal), PRIVATE, FINAL)
            .initializer("new $T()", updaterType).build());
      }
      if (details.goalOptions.builder) {
        ClassName stepsType = builderImplType(goal);
        builder.add(FieldSpec.builder(stepsType,
            stepsField(goal), PRIVATE, FINAL)
            .initializer("new $T()", stepsType).build());
      }
    }
    return builder.build();
  }

  private static final Function<AbstractGoalContext, BuilderMethod> goalToToBuilder
      = goalCases(GeneratorV.goalToToBuilder, GeneratorB.goalToToBuilder);

  private static final Function<AbstractGoalContext, BuilderMethod> goalToBuilder
      = goalCases(GeneratorV.goalToBuilder, GeneratorB.goalToBuilder);

  private static ImmutableList<AbstractGoalContext> goals(final Goals goals) {
    return FluentIterable.from(goals.goals)
        .transform(new Function<IGoal, AbstractGoalContext>() {
          @Override
          public AbstractGoalContext apply(IGoal goal) {
            return goal.withContext(goals.buildersContext);
          }
        })
        .toList();
  }

  static String updaterField(AbstractGoalContext goal) {
    return downcase(goalName.apply(goal) + "Updater");
  }

  static String stepsField(AbstractGoalContext goal) {
    return downcase(goalName.apply(goal) + "BuilderImpl");
  }

  private static final class Goals {
    private final DtoBuildersContext.BuildersContext buildersContext;
    private final ImmutableList<? extends IGoal> goals;

    private Goals(DtoBuildersContext.BuildersContext buildersContext,
                  List<? extends IGoal> goals) {
      this.buildersContext = buildersContext;
      this.goals = ImmutableList.copyOf(goals);
    }
  }
}
