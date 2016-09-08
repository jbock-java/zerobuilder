package net.zerobuilder.compiler;

import com.google.common.base.Function;
import com.google.common.base.Optional;
import com.google.common.base.Predicate;
import com.google.common.collect.FluentIterable;
import com.google.common.collect.ImmutableList;
import com.squareup.javapoet.ClassName;
import com.squareup.javapoet.FieldSpec;
import com.squareup.javapoet.MethodSpec;
import com.squareup.javapoet.ParameterSpec;
import com.squareup.javapoet.ParameterizedTypeName;
import com.squareup.javapoet.TypeName;
import com.squareup.javapoet.TypeSpec;
import net.zerobuilder.compiler.Analyser.AnalysisResult;

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
import static net.zerobuilder.compiler.GoalContext.maybeAddPublic;
import static net.zerobuilder.compiler.Messages.JavadocMessages.generatedAnnotations;
import static net.zerobuilder.compiler.Utilities.downcase;

final class Generator {

  private final Elements elements;

  private static final String STATIC_FIELD_INSTANCE = "INSTANCE";

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
        .addTypes(builderImpls(analysisResult.goals))
        .build();
  }

  private ImmutableList<TypeSpec> builderImpls(ImmutableList<UberGoalContext> goals) {
    ImmutableList.Builder<TypeSpec> builder = ImmutableList.builder();
    for (UberGoalContext goal : goals) {
      builder.add(goal.builderImpl());
    }
    return builder.build();
  }

  private Optional<FieldSpec> threadLocalField(BuildConfig config) {
    if (!config.nogc) {
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
    return Optional.of(FieldSpec.builder(threadLocal, STATIC_FIELD_INSTANCE)
        .initializer("$L", anonymousClassBuilder("")
            .addSuperinterface(threadLocal)
            .addMethod(initialValue)
            .build())
        .addModifiers(PRIVATE, STATIC, FINAL)
        .build());
  }

  private ImmutableList<MethodSpec> toBuilderMethods(final AnalysisResult analysisResult) {
    return FluentIterable.from(analysisResult.goals).filter(new Predicate<UberGoalContext>() {
      @Override
      public boolean apply(UberGoalContext context) {
        return context.goal.toBuilder;
      }
    }).transform(new Function<UberGoalContext, MethodSpec>() {
      @Override
      public MethodSpec apply(UberGoalContext context) {
        GoalContext goal = context.goal;
        String parameterName = downcase(goal.goalTypeName());
        String methodName = downcase(goal.goalName + "ToBuilder");
        MethodSpec.Builder builder = methodBuilder(methodName)
            .addParameter(goal.goalType, parameterName);
        String varUpdater = "updater";
        ClassName updaterType = context.updaterContext.typeName();
        if (analysisResult.config.nogc) {
          builder.addStatement("$T $L = $L.get().$N", updaterType, varUpdater,
              STATIC_FIELD_INSTANCE, updaterField(context));
        } else {
          builder.addStatement("$T $L = new $T()", updaterType, varUpdater,
              updaterType);
        }
        for (ParameterContext parameter : goal.goalParameters) {
          if (parameter.projectionMethodName.isPresent()) {
            builder.addStatement("$N.$N = $N.$N()", varUpdater, parameter.name,
                parameterName, parameter.projectionMethodName.get());
          } else {
            builder.addStatement("$N.$N = $N.$N", varUpdater, parameter.name,
                parameterName, parameter.name);
          }
        }
        builder.addStatement("return $L", varUpdater);
        return builder
            .returns(goal.contractUpdaterName())
            .addModifiers(goal.maybeAddPublic(STATIC)).build();
      }
    }).toList();
  }

  private ImmutableList<MethodSpec> builderMethods(AnalysisResult analysisResult) {
    ImmutableList.Builder<MethodSpec> builder = ImmutableList.builder();
    for (UberGoalContext context : analysisResult.goals) {
      GoalContext goal = context.goal;
      ParameterContext firstStep = goal.goalParameters.get(0);
      Optional<ClassName> maybeReceiver = goal.receiverType();
      MethodSpec.Builder spec = methodBuilder(
          downcase(goal.goalName + "Builder"));
      if (maybeReceiver.isPresent()) {
        ClassName receiver = maybeReceiver.get();
        spec.addParameter(ParameterSpec.builder(receiver,
            downcase(receiver.simpleName())).build());
        if (analysisResult.config.nogc) {
          spec.addStatement("$T $N = $N.get().$N", goal.stepsImplTypeName(),
              downcase(goal.stepsImplTypeName().simpleName()),
              STATIC_FIELD_INSTANCE, stepsField(context));
        } else {
          spec.addStatement("$T $N = new $T()", goal.stepsImplTypeName(),
              downcase(goal.stepsImplTypeName().simpleName()),
              goal.stepsImplTypeName());
        }
        spec.addStatement("$N.$N = $N", downcase(goal.stepsImplTypeName().simpleName()),
            "_" + downcase(receiver.simpleName()),
            downcase(receiver.simpleName()));
        spec.addStatement("return $N",
            downcase(goal.stepsImplTypeName().simpleName()));
      } else {
        if (analysisResult.config.nogc) {
          spec.addStatement("return $N.get().$N", STATIC_FIELD_INSTANCE,
              stepsField(context));
        } else {
          spec.addStatement("return new $T()", goal.stepsImplTypeName());
        }
      }
      builder.add(spec.returns(firstStep.stepContract)
          .addModifiers(goal.maybeAddPublic(STATIC))
          .build());
    }
    return builder.build();
  }

  private ImmutableList<FieldSpec> instanceFields(AnalysisResult analysisResult) {
    if (!analysisResult.config.nogc) {
      return ImmutableList.of();
    }
    ImmutableList.Builder<FieldSpec> builder = ImmutableList.builder();
    for (UberGoalContext goal : analysisResult.goals) {
      if (goal.goal.toBuilder) {
        builder.add(FieldSpec.builder(goal.updaterContext.typeName(),
            downcase(goal.goal.goalName + "Updater"), PRIVATE, FINAL)
            .initializer("new $T()", goal.updaterContext.typeName()).build());
      }
      builder.add(FieldSpec.builder(goal.goal.stepsImplTypeName(),
          downcase(goal.goal.goalName + "Steps"), PRIVATE, FINAL)
          .initializer("new $T()", goal.goal.stepsImplTypeName()).build());
    }
    return builder.build();
  }

  private String updaterField(UberGoalContext context) {
    return downcase(context.goal.goalName + "Updater");
  }

  private String stepsField(UberGoalContext context) {
    return downcase(context.goal.goalName + "Steps");
  }

}