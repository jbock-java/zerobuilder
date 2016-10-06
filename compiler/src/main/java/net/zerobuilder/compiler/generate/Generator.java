package net.zerobuilder.compiler.generate;

import com.google.common.base.Function;
import com.google.common.base.Predicate;
import com.google.common.collect.FluentIterable;
import com.google.common.collect.ImmutableList;
import com.squareup.javapoet.ClassName;
import com.squareup.javapoet.FieldSpec;
import com.squareup.javapoet.MethodSpec;
import com.squareup.javapoet.TypeSpec;
import net.zerobuilder.compiler.analyse.DtoGoal.AbstractGoal;
import net.zerobuilder.compiler.generate.DtoGoalContext.AbstractGoalContext;

import javax.lang.model.util.Elements;
import java.util.List;

import static com.squareup.javapoet.MethodSpec.constructorBuilder;
import static com.squareup.javapoet.TypeSpec.classBuilder;
import static javax.lang.model.element.Modifier.FINAL;
import static javax.lang.model.element.Modifier.PRIVATE;
import static javax.lang.model.element.Modifier.PUBLIC;
import static net.zerobuilder.compiler.Messages.JavadocMessages.generatedAnnotations;
import static net.zerobuilder.compiler.Utilities.downcase;
import static net.zerobuilder.compiler.generate.BuilderContext.defineBuilderImpl;
import static net.zerobuilder.compiler.generate.BuilderContext.defineContract;
import static net.zerobuilder.compiler.generate.DtoGoalContext.builderImplType;
import static net.zerobuilder.compiler.generate.DtoGoalContext.goalCases;
import static net.zerobuilder.compiler.generate.DtoGoalContext.goalName;
import static net.zerobuilder.compiler.generate.UpdaterContext.defineUpdater;
import static net.zerobuilder.compiler.generate.UpdaterContext.updaterType;

/**
 * Generates a class {@code FooBuilders} for each {@link net.zerobuilder.Builders} annotated class {@code Foo}.
 */
public final class Generator {

  private final Elements elements;

  public Generator(Elements elements) {
    this.elements = elements;
  }

  public TypeSpec generate(Goals analysisResult) {
    ImmutableList<AbstractGoalContext> goals = goals(analysisResult);
    return classBuilder(analysisResult.buildersContext.generatedType)
        .addFields(instanceFields(analysisResult, goals))
        .addMethod(constructorBuilder().addModifiers(PRIVATE).build())
        .addFields(analysisResult.buildersContext.recycle
            ? ImmutableList.of(analysisResult.buildersContext.cache)
            : ImmutableList.<FieldSpec>of())
        .addMethods(builderMethods(goals))
        .addMethods(toBuilderMethods(goals))
        .addAnnotations(generatedAnnotations(elements))
        .addModifiers(PUBLIC, FINAL)
        .addTypes(nestedGoalTypes(goals))
        .build();
  }

  private ImmutableList<TypeSpec> nestedGoalTypes(ImmutableList<AbstractGoalContext> goals) {
    ImmutableList.Builder<TypeSpec> builder = ImmutableList.builder();
    for (AbstractGoalContext goal : goals) {
      AbstractGoal abstractGoal = DtoGoalContext.abstractGoal.apply(goal);
      if (abstractGoal.goalOptions.toBuilder) {
        builder.add(defineUpdater(goal));
      }
      if (abstractGoal.goalOptions.builder) {
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
            AbstractGoal abstractGoal = DtoGoalContext.abstractGoal.apply(goal);
            return abstractGoal.goalOptions.toBuilder;
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
            AbstractGoal abstractGoal = DtoGoalContext.abstractGoal.apply(goal);
            return abstractGoal.goalOptions.builder;
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
      AbstractGoal abstractGoal = DtoGoalContext.abstractGoal.apply(goal);
      if (abstractGoal.goalOptions.toBuilder) {
        ClassName updaterType = updaterType(goal);
        builder.add(FieldSpec.builder(updaterType,
            updaterField(goal), PRIVATE, FINAL)
            .initializer("new $T()", updaterType).build());
      }
      if (abstractGoal.goalOptions.builder) {
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

  private ImmutableList<DtoGoalContext.AbstractGoalContext> goals(final Goals goals) {
    return FluentIterable.from(goals.goals)
        .transform(new Function<DtoGoalContext.IGoal, DtoGoalContext.AbstractGoalContext>() {
          @Override
          public DtoGoalContext.AbstractGoalContext apply(DtoGoalContext.IGoal goal) {
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

  public static final class Goals {
    public final DtoBuilders.BuildersContext buildersContext;
    public final ImmutableList<? extends DtoGoalContext.IGoal> goals;

    public Goals(DtoBuilders.BuildersContext buildersContext,
                 List<? extends DtoGoalContext.IGoal> goals) {
      this.buildersContext = buildersContext;
      this.goals = ImmutableList.copyOf(goals);
    }
  }
}
