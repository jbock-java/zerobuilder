package net.zerobuilder.compiler.generate;

import com.google.common.base.Function;
import com.google.common.base.Predicate;
import com.google.common.collect.FluentIterable;
import com.google.common.collect.ImmutableList;
import com.squareup.javapoet.ClassName;
import com.squareup.javapoet.FieldSpec;
import com.squareup.javapoet.MethodSpec;
import com.squareup.javapoet.TypeSpec;
import net.zerobuilder.compiler.analyse.Analyser.AnalysisResult;
import net.zerobuilder.compiler.generate.DtoGoalContext.AbstractGoalContext;

import javax.lang.model.util.Elements;

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

  public TypeSpec generate(AnalysisResult analysisResult) {
    return classBuilder(analysisResult.builders.generatedType)
        .addFields(instanceFields(analysisResult))
        .addMethod(constructorBuilder().addModifiers(PRIVATE).build())
        .addFields(analysisResult.builders.recycle
            ? ImmutableList.of(analysisResult.builders.cache)
            : ImmutableList.<FieldSpec>of())
        .addMethods(builderMethods(analysisResult))
        .addMethods(toBuilderMethods(analysisResult))
        .addAnnotations(generatedAnnotations(elements))
        .addModifiers(PUBLIC, FINAL)
        .addTypes(nestedGoalTypes(analysisResult.goals))
        .build();
  }

  private ImmutableList<TypeSpec> nestedGoalTypes(ImmutableList<AbstractGoalContext> goals) {
    ImmutableList.Builder<TypeSpec> builder = ImmutableList.builder();
    for (AbstractGoalContext goal : goals) {
      if (goal.toBuilder) {
        builder.add(defineUpdater(goal));
      }
      if (goal.builder) {
        builder.add(defineBuilderImpl(goal));
        builder.add(defineContract(goal));
      }
    }
    return builder.build();
  }

  private ImmutableList<MethodSpec> toBuilderMethods(AnalysisResult analysisResult) {
    return FluentIterable.from(analysisResult.goals)
        .filter(new Predicate<AbstractGoalContext>() {
          @Override
          public boolean apply(AbstractGoalContext goal) {
            return goal.toBuilder;
          }
        })
        .transform(goalToToBuilder)
        .toList();
  }

  private ImmutableList<MethodSpec> builderMethods(AnalysisResult analysisResult) {
    return FluentIterable.from(analysisResult.goals)
        .filter(new Predicate<AbstractGoalContext>() {
          @Override
          public boolean apply(AbstractGoalContext goal) {
            return goal.builder;
          }
        })
        .transform(goalToBuilder)
        .toList();
  }

  private ImmutableList<FieldSpec> instanceFields(AnalysisResult analysisResult) {
    if (!analysisResult.builders.recycle) {
      return ImmutableList.of();
    }
    ImmutableList.Builder<FieldSpec> builder = ImmutableList.builder();
    for (AbstractGoalContext goal : analysisResult.goals) {
      if (goal.toBuilder) {
        ClassName updaterType = updaterType(goal);
        builder.add(FieldSpec.builder(updaterType,
            updaterField(goal), PRIVATE, FINAL)
            .initializer("new $T()", updaterType).build());
      }
      if (goal.builder) {
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

  static String updaterField(AbstractGoalContext goal) {
    return downcase(goalName.apply(goal) + "Updater");
  }

  static String stepsField(AbstractGoalContext goal) {
    return downcase(goalName.apply(goal) + "BuilderImpl");
  }
}
