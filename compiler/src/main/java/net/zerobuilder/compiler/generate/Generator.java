package net.zerobuilder.compiler.generate;

import com.google.common.base.Function;
import com.google.common.base.Optional;
import com.google.common.base.Predicate;
import com.google.common.collect.FluentIterable;
import com.google.common.collect.ImmutableList;
import com.squareup.javapoet.ClassName;
import com.squareup.javapoet.FieldSpec;
import com.squareup.javapoet.MethodSpec;
import com.squareup.javapoet.ParameterizedTypeName;
import com.squareup.javapoet.TypeName;
import com.squareup.javapoet.TypeSpec;
import net.zerobuilder.compiler.analyse.Analyser.AnalysisResult;
import net.zerobuilder.compiler.generate.DtoBuilders.BuildersContext;
import net.zerobuilder.compiler.generate.DtoGoalContext.AbstractGoalContext;
import net.zerobuilder.compiler.generate.DtoGoalContext.GoalCases;

import javax.lang.model.util.Elements;

import static com.google.common.base.Optional.absent;
import static com.google.common.base.Optional.presentInstances;
import static com.google.common.collect.ImmutableList.of;
import static com.squareup.javapoet.MethodSpec.constructorBuilder;
import static com.squareup.javapoet.MethodSpec.methodBuilder;
import static com.squareup.javapoet.TypeSpec.anonymousClassBuilder;
import static com.squareup.javapoet.TypeSpec.classBuilder;
import static javax.lang.model.element.Modifier.FINAL;
import static javax.lang.model.element.Modifier.PRIVATE;
import static javax.lang.model.element.Modifier.PROTECTED;
import static javax.lang.model.element.Modifier.PUBLIC;
import static javax.lang.model.element.Modifier.STATIC;
import static net.zerobuilder.compiler.Messages.JavadocMessages.generatedAnnotations;
import static net.zerobuilder.compiler.Utilities.downcase;
import static net.zerobuilder.compiler.generate.BuilderContext.defineBuilderImpl;
import static net.zerobuilder.compiler.generate.BuilderContext.defineContract;
import static net.zerobuilder.compiler.generate.DtoGoalContext.builderImplName;
import static net.zerobuilder.compiler.generate.DtoGoalContext.getGoalName;
import static net.zerobuilder.compiler.generate.DtoGoalContext.goalCases;
import static net.zerobuilder.compiler.generate.DtoGoalContext.goalCasesFunction;
import static net.zerobuilder.compiler.generate.UpdaterContext.defineUpdater;

/**
 * Generates an xyzBuilders class for each {@link net.zerobuilder.Builders} annotated class Xyz.
 */
public final class Generator {

  private final Elements elements;

  /**
   * Name of a {@code static ThreadLocal } that holds an instance of the generated type, if {@code recycle}.
   */
  static final String TL = "INSTANCE";

  public Generator(Elements elements) {
    this.elements = elements;
  }

  public TypeSpec generate(AnalysisResult analysisResult) {
    return classBuilder(analysisResult.builders.generatedType)
        .addFields(instanceFields(analysisResult))
        .addMethod(constructorBuilder().addModifiers(PRIVATE).build())
        .addFields(presentInstances(of(threadLocalField(analysisResult.builders))))
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

  private Optional<FieldSpec> threadLocalField(BuildersContext builders) {
    if (!builders.recycle) {
      return absent();
    }
    ClassName generatedTypeName = builders.generatedType;
    TypeName threadLocal = ParameterizedTypeName.get(ClassName.get(ThreadLocal.class),
        generatedTypeName);
    MethodSpec initialValue = methodBuilder("initialValue")
        .addAnnotation(Override.class)
        .addModifiers(PROTECTED)
        .returns(generatedTypeName)
        .addStatement("return new $T()", generatedTypeName)
        .build();
    return Optional.of(FieldSpec.builder(threadLocal, TL)
        .initializer("$L", anonymousClassBuilder("")
            .addSuperinterface(threadLocal)
            .addMethod(initialValue)
            .build())
        .addModifiers(PRIVATE, STATIC, FINAL)
        .build());
  }

  private ImmutableList<MethodSpec> toBuilderMethods(AnalysisResult analysisResult) {
    return FluentIterable.from(analysisResult.goals)
        .filter(new Predicate<AbstractGoalContext>() {
          @Override
          public boolean apply(AbstractGoalContext goal) {
            return goal.toBuilder;
          }
        })
        .transform(goalCasesFunction(goalToToBuilder))
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
        .transform(new Function<AbstractGoalContext, MethodSpec>() {
          public MethodSpec apply(AbstractGoalContext goal) {
            return goal
                .accept(goalToBuilder);
          }
        })
        .toList();
  }

  private ImmutableList<FieldSpec> instanceFields(AnalysisResult analysisResult) {
    if (!analysisResult.builders.recycle) {
      return ImmutableList.of();
    }
    ImmutableList.Builder<FieldSpec> builder = ImmutableList.builder();
    for (AbstractGoalContext goal : analysisResult.goals) {
      if (goal.toBuilder) {
        ClassName updaterType = goal.accept(UpdaterContext.typeName);
        builder.add(FieldSpec.builder(updaterType,
            updaterField(goal), PRIVATE, FINAL)
            .initializer("new $T()", updaterType).build());
      }
      if (goal.builder) {
        ClassName stepsType = goal.accept(builderImplName);
        builder.add(FieldSpec.builder(stepsType,
            stepsField(goal), PRIVATE, FINAL)
            .initializer("new $T()", stepsType).build());
      }
    }
    return builder.build();
  }

  private static final GoalCases<MethodSpec> goalToToBuilder
      = goalCases(GeneratorV.goalToToBuilder, GeneratorB.goalToToBuilder);

  private static final GoalCases<MethodSpec> goalToBuilder
      = goalCases(GeneratorV.goalToBuilder, GeneratorB.goalToBuilder);

  static String updaterField(AbstractGoalContext goal) {
    return downcase(goal.accept(getGoalName) + "Updater");
  }

  static String stepsField(AbstractGoalContext goal) {
    return downcase(goal.accept(getGoalName) + "BuilderImpl");
  }
}
