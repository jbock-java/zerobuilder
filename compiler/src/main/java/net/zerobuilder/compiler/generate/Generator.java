package net.zerobuilder.compiler.generate;

import com.google.common.base.Function;
import com.google.common.base.Predicate;
import com.google.common.collect.FluentIterable;
import com.google.common.collect.ImmutableList;
import com.squareup.javapoet.AnnotationSpec;
import com.squareup.javapoet.ClassName;
import com.squareup.javapoet.FieldSpec;
import com.squareup.javapoet.MethodSpec;
import com.squareup.javapoet.TypeSpec;
import net.zerobuilder.compiler.analyse.Analyser.ValidGoals;
import net.zerobuilder.compiler.generate.DtoGoal.AbstractGoalDetails;
import net.zerobuilder.compiler.generate.DtoGoalContext.AbstractGoalContext;
import net.zerobuilder.compiler.generate.DtoGoalContext.IGoal;
import net.zerobuilder.compiler.generate.DtoValidGoal.ValidGoal;

import java.util.List;

import static com.squareup.javapoet.MethodSpec.constructorBuilder;
import static com.squareup.javapoet.TypeSpec.classBuilder;
import static javax.lang.model.element.Modifier.FINAL;
import static javax.lang.model.element.Modifier.PRIVATE;
import static javax.lang.model.element.Modifier.PUBLIC;
import static net.zerobuilder.compiler.Utilities.downcase;
import static net.zerobuilder.compiler.generate.BuilderContext.defineBuilderImpl;
import static net.zerobuilder.compiler.generate.BuilderContext.defineContract;
import static net.zerobuilder.compiler.generate.DtoGoalContext.builderImplType;
import static net.zerobuilder.compiler.generate.DtoGoalContext.goalCases;
import static net.zerobuilder.compiler.generate.DtoGoalContext.goalName;
import static net.zerobuilder.compiler.generate.GoalContextFactory.prepareGoal;
import static net.zerobuilder.compiler.generate.UpdaterContext.defineUpdater;
import static net.zerobuilder.compiler.generate.UpdaterContext.updaterType;

/**
 * Generates a class {@code FooBuilders} for each {@link net.zerobuilder.Builders} annotated class {@code Foo}.
 */
public final class Generator {

  private final ImmutableList<AnnotationSpec> generatedAnnotations;

  public Generator(ImmutableList<AnnotationSpec> generatedAnnotations) {
    this.generatedAnnotations = generatedAnnotations;
  }

  public TypeSpec generate(ValidGoals goals) {
    Function<ValidGoal, IGoal> prepare = prepareGoal(goals.buildersContext.generatedType);
    ImmutableList.Builder<IGoal> builder = ImmutableList.builder();
    for (ValidGoal validGoal : goals.validGoals) {
      builder.add(prepare.apply(validGoal));
    }
    return generate(new Goals(goals.buildersContext, builder.build()));
  }

  private TypeSpec generate(Goals analysisResult) {
    ImmutableList<AbstractGoalContext> goals = goals(analysisResult);
    return classBuilder(analysisResult.buildersContext.generatedType)
        .addFields(instanceFields(analysisResult, goals))
        .addMethod(constructorBuilder().addModifiers(PRIVATE).build())
        .addFields(analysisResult.buildersContext.recycle
            ? ImmutableList.of(analysisResult.buildersContext.cache)
            : ImmutableList.<FieldSpec>of())
        .addMethods(builderMethods(goals))
        .addMethods(toBuilderMethods(goals))
        .addAnnotations(generatedAnnotations)
        .addModifiers(PUBLIC, FINAL)
        .addTypes(nestedGoalTypes(goals))
        .build();
  }

  private ImmutableList<TypeSpec> nestedGoalTypes(ImmutableList<AbstractGoalContext> goals) {
    ImmutableList.Builder<TypeSpec> builder = ImmutableList.builder();
    for (AbstractGoalContext goal : goals) {
      AbstractGoalDetails abstractGoalDetails = DtoGoalContext.abstractGoal.apply(goal);
      if (abstractGoalDetails.goalOptions.toBuilder) {
        builder.add(defineUpdater(goal));
      }
      if (abstractGoalDetails.goalOptions.builder) {
        builder.add(defineBuilderImpl(goal));
        builder.add(defineContract(goal));
      }
    }
    return builder.build();
  }

  private ImmutableList<MethodSpec> toBuilderMethods(ImmutableList<AbstractGoalContext> goals) {
    return FluentIterable.from(goals)
        .filter(new Predicate<AbstractGoalContext>() {
          @Override
          public boolean apply(AbstractGoalContext goal) {
            AbstractGoalDetails abstractGoalDetails = DtoGoalContext.abstractGoal.apply(goal);
            return abstractGoalDetails.goalOptions.toBuilder;
          }
        })
        .transform(goalToToBuilder)
        .toList();
  }

  private ImmutableList<MethodSpec> builderMethods(ImmutableList<AbstractGoalContext> goals) {
    return FluentIterable.from(goals)
        .filter(new Predicate<AbstractGoalContext>() {
          @Override
          public boolean apply(AbstractGoalContext goal) {
            AbstractGoalDetails abstractGoalDetails = DtoGoalContext.abstractGoal.apply(goal);
            return abstractGoalDetails.goalOptions.builder;
          }
        })
        .transform(goalToBuilder)
        .toList();
  }

  private ImmutableList<FieldSpec> instanceFields(Goals analysisResult,
                                                  ImmutableList<AbstractGoalContext> goals) {
    if (!analysisResult.buildersContext.recycle) {
      return ImmutableList.of();
    }
    ImmutableList.Builder<FieldSpec> builder = ImmutableList.builder();
    for (AbstractGoalContext goal : goals) {
      AbstractGoalDetails abstractGoalDetails = DtoGoalContext.abstractGoal.apply(goal);
      if (abstractGoalDetails.goalOptions.toBuilder) {
        ClassName updaterType = updaterType(goal);
        builder.add(FieldSpec.builder(updaterType,
            updaterField(goal), PRIVATE, FINAL)
            .initializer("new $T()", updaterType).build());
      }
      if (abstractGoalDetails.goalOptions.builder) {
        ClassName stepsType = builderImplType(goal);
        builder.add(FieldSpec.builder(stepsType,
            stepsField(goal), PRIVATE, FINAL)
            .initializer("new $T()", stepsType).build());
      }
    }
    return builder.build();
  }

  private static final Function<AbstractGoalContext, MethodSpec> goalToToBuilder
      = goalCases(GeneratorV.goalToToBuilder, GeneratorB.goalToToBuilder);

  private static final Function<AbstractGoalContext, MethodSpec> goalToBuilder
      = goalCases(GeneratorV.goalToBuilder, GeneratorB.goalToBuilder);

  private ImmutableList<AbstractGoalContext> goals(final Goals goals) {
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
    private final DtoBuilders.BuildersContext buildersContext;
    private final ImmutableList<? extends IGoal> goals;

    private Goals(DtoBuilders.BuildersContext buildersContext,
                  List<? extends IGoal> goals) {
      this.buildersContext = buildersContext;
      this.goals = ImmutableList.copyOf(goals);
    }
  }
}
