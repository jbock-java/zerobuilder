package net.zerobuilder.compiler;

import com.google.common.base.Function;
import com.google.common.base.Optional;
import com.google.common.base.Predicate;
import com.google.common.collect.FluentIterable;
import com.google.common.collect.ImmutableList;
import com.squareup.javapoet.ClassName;
import com.squareup.javapoet.CodeBlock;
import com.squareup.javapoet.FieldSpec;
import com.squareup.javapoet.MethodSpec;
import com.squareup.javapoet.ParameterSpec;
import com.squareup.javapoet.ParameterizedTypeName;
import com.squareup.javapoet.TypeName;
import com.squareup.javapoet.TypeSpec;
import net.zerobuilder.compiler.Analyser.AnalysisResult;
import net.zerobuilder.compiler.GoalContext.GoalCases;
import net.zerobuilder.compiler.GoalContextFactory.GoalKind;

import javax.lang.model.element.Modifier;
import javax.lang.model.util.Elements;

import static com.google.common.base.Optional.absent;
import static com.google.common.base.Optional.presentInstances;
import static com.google.common.collect.ImmutableList.of;
import static com.google.common.collect.Iterables.toArray;
import static com.squareup.javapoet.MethodSpec.constructorBuilder;
import static com.squareup.javapoet.MethodSpec.methodBuilder;
import static com.squareup.javapoet.TypeSpec.anonymousClassBuilder;
import static com.squareup.javapoet.TypeSpec.classBuilder;
import static javax.lang.model.element.Modifier.FINAL;
import static javax.lang.model.element.Modifier.PRIVATE;
import static javax.lang.model.element.Modifier.PROTECTED;
import static javax.lang.model.element.Modifier.STATIC;
import static net.zerobuilder.compiler.ContractContext.buildContract;
import static net.zerobuilder.compiler.GoalContext.goalCasesFunction;
import static net.zerobuilder.compiler.GoalContext.maybeAddPublic;
import static net.zerobuilder.compiler.GoalContext.builderImplName;
import static net.zerobuilder.compiler.GoalContextFactory.GoalKind.INSTANCE_METHOD;
import static net.zerobuilder.compiler.Messages.JavadocMessages.generatedAnnotations;
import static net.zerobuilder.compiler.StepsContext.buildStepsImpl;
import static net.zerobuilder.compiler.UpdaterContext.buildUpdaterImpl;
import static net.zerobuilder.compiler.Utilities.downcase;
import static net.zerobuilder.compiler.Utilities.upcase;

/**
 * Generates an xyzBuilders class for each {@link net.zerobuilder.Builders} annotated class Xyz.
 */
final class Generator {

  private final Elements elements;

  /**
   * Name of a {@code static ThreadLocal } that holds an instance of the generated type, if {@code recycle}.
   */
  private static final String TL = "INSTANCE";

  Generator(Elements elements) {
    this.elements = elements;
  }

  TypeSpec generate(AnalysisResult analysisResult) {
    return classBuilder(analysisResult.config.generatedType)
        .addFields(instanceFields(analysisResult))
        .addMethod(constructorBuilder().addModifiers(PRIVATE).build())
        .addFields(presentInstances(of(threadLocalField(analysisResult.config))))
        .addMethods(builderMethods(analysisResult))
        .addMethods(toBuilderMethods(analysisResult))
        .addAnnotations(generatedAnnotations(elements))
        .addModifiers(toArray(maybeAddPublic(analysisResult.config.isPublic, FINAL), Modifier.class))
        .addTypes(nestedGoalTypes(analysisResult.goals))
        .build();
  }

  private ImmutableList<TypeSpec> nestedGoalTypes(ImmutableList<GoalContext> goals) {
    ImmutableList.Builder<TypeSpec> builder = ImmutableList.builder();
    for (GoalContext goal : goals) {
      if (goal.toBuilder) {
        builder.add(buildUpdaterImpl(goal));
      }
      if (goal.builder) {
        builder.add(buildStepsImpl(goal));
        builder.add(buildContract(goal));
      }
    }
    return builder.build();
  }

  private Optional<FieldSpec> threadLocalField(BuilderContext config) {
    if (!config.recycle) {
      return absent();
    }
    ClassName generatedTypeName = config.generatedType;
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
        .filter(new Predicate<GoalContext>() {
          @Override
          public boolean apply(GoalContext goal) {
            return goal.toBuilder;
          }
        })
        .transform(goalCasesFunction(goalToToBuilder))
        .toList();
  }

  private ImmutableList<MethodSpec> builderMethods(AnalysisResult analysisResult) {
    return FluentIterable.from(analysisResult.goals)
        .filter(new Predicate<GoalContext>() {
          @Override
          public boolean apply(GoalContext goal) {
            return goal.builder;
          }
        })
        .transform(new Function<GoalContext, MethodSpec>() {
          public MethodSpec apply(GoalContext goal) {
            return goal
                .accept(goalToBuilder);
          }
        })
        .toList();
  }

  private ImmutableList<FieldSpec> instanceFields(AnalysisResult analysisResult) {
    if (!analysisResult.config.recycle) {
      return ImmutableList.of();
    }
    ImmutableList.Builder<FieldSpec> builder = ImmutableList.builder();
    for (GoalContext goal : analysisResult.goals) {
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

  private static final GoalCases<MethodSpec> goalToToBuilder = new GoalCases<MethodSpec>() {
    @Override
    MethodSpec regularGoal(GoalContext goal, TypeName goalType, GoalKind kind) {
      String instance = downcase(((ClassName) goalType.box()).simpleName());
      String methodName = downcase(goal.goalName + "ToBuilder");
      MethodSpec.Builder method = methodBuilder(methodName)
          .addParameter(goalType, instance);
      String updater = "updater";
      ClassName updaterType = goal.accept(UpdaterContext.typeName);
      if (goal.config.recycle) {
        method.addStatement("$T $L = $L.get().$N", updaterType, updater,
            TL, updaterField(goal));
      } else {
        method.addStatement("$T $L = new $T()", updaterType, updater,
            updaterType);
      }
      CodeBlock.Builder builder = CodeBlock.builder();
      for (ParameterContext parameter : goal.goalParameters) {
        if (parameter.validParameter.projectionMethodName.isPresent()) {
          builder.addStatement("$N.$N = $N.$N()", updater, parameter.validParameter.name,
              instance, parameter.validParameter.projectionMethodName.get());
        } else {
          builder.addStatement("$N.$N = $N.$N", updater, parameter.validParameter.name,
              instance, parameter.validParameter.name);
        }
      }
      method.addCode(builder.build());
      method.addStatement("return $L", updater);
      return method
          .returns(goal.accept(UpdaterContext.typeName))
          .addModifiers(goal.maybeAddPublic(STATIC)).build();
    }
    @Override
    MethodSpec fieldGoal(GoalContext goal, ClassName goalType) {
      String instance = downcase(goalType.simpleName());
      String methodName = downcase(goal.goalName + "ToBuilder");
      CodeBlock.Builder builder = CodeBlock.builder();
      MethodSpec.Builder method = methodBuilder(methodName)
          .addParameter(goalType, instance);
      String updater = "updater";
      ClassName updaterType = goal.accept(UpdaterContext.typeName);
      if (goal.config.recycle) {
        method.addStatement("$T $L = $L.get().$N", updaterType, updater,
            TL, updaterField(goal));
      } else {
        method.addStatement("$T $L = new $T()", updaterType, updater,
            updaterType);
      }
      builder.addStatement("$N.$N = new $T()", updater, instance, goalType);
      for (ParameterContext parameter : goal.goalParameters) {
        String parameterName = upcase(parameter.validParameter.name);
        builder.addStatement("$N.$N.set$L($N.$L())", updater,
            instance,
            parameterName,
            instance,
            parameter.validParameter.projectionMethodName.get());
      }
      method.addCode(builder.build());
      method.addStatement("return $L", updater);
      return method
          .returns(goal.accept(UpdaterContext.typeName))
          .addModifiers(goal.maybeAddPublic(STATIC)).build();
    }
  };

  private static final GoalCases<MethodSpec> goalToBuilder = new GoalCases<MethodSpec>() {
    @Override
    MethodSpec regularGoal(GoalContext goal, TypeName goalType, GoalKind kind) {
      ClassName stepsType = goal.accept(builderImplName);
      MethodSpec.Builder method = methodBuilder(goal.goalName + "Builder")
          .returns(goal.goalParameters.get(0).typeName)
          .addModifiers(goal.maybeAddPublic(STATIC));
      String steps = downcase(stepsType.simpleName());
      method.addCode(goal.config.recycle
          ? CodeBlock.of("$T $N = $N.get().$N;\n", stepsType, steps, TL, stepsField(goal))
          : CodeBlock.of("$T $N = new $T();\n", stepsType, steps, stepsType));
      if (kind != INSTANCE_METHOD) {
        return method.addStatement("return $N", steps).build();
      }
      ClassName instanceType = goal.config.annotatedType;
      String instance = downcase(instanceType.simpleName());
      return method
          .addParameter(ParameterSpec.builder(instanceType, instance).build())
          .addStatement("$N.$N = $N", steps, '_' + instance, instance)
          .addStatement("return $N", steps)
          .build();
    }
    @Override
    MethodSpec fieldGoal(GoalContext goal, ClassName goalType) {
      ClassName stepsType = goal.accept(builderImplName);
      MethodSpec.Builder method = methodBuilder(goal.goalName + "Builder")
          .returns(goal.goalParameters.get(0).typeName)
          .addModifiers(goal.maybeAddPublic(STATIC));
      String steps = downcase(stepsType.simpleName());
      method.addCode(goal.config.recycle
          ? CodeBlock.of("$T $N = $N.get().$N;\n", stepsType, steps, TL, stepsField(goal))
          : CodeBlock.of("$T $N = new $T();\n", stepsType, steps, stepsType));
      return method.addStatement("$N.$N = new $T()", steps, downcase(goalType.simpleName()), goalType)
          .addStatement("return $N", steps)
          .build();
    }
  };

  private static String updaterField(GoalContext goal) {
    return downcase(goal.goalName + "Updater");
  }

  private static String stepsField(GoalContext goal) {
    return downcase(goal.goalName + "BuilderImpl");
  }

}
