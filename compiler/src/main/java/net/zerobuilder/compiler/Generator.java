package net.zerobuilder.compiler;

import com.google.common.base.Optional;
import com.google.common.collect.ImmutableList;
import com.squareup.javapoet.ClassName;
import com.squareup.javapoet.FieldSpec;
import com.squareup.javapoet.MethodSpec;
import com.squareup.javapoet.ParameterSpec;
import com.squareup.javapoet.ParameterizedTypeName;
import com.squareup.javapoet.TypeName;
import com.squareup.javapoet.TypeSpec;
import net.zerobuilder.compiler.Analyzer.AnalysisResult;

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
import static net.zerobuilder.compiler.GoalContext.SharedGoalContext.maybeAddPublic;
import static net.zerobuilder.compiler.Messages.JavadocMessages.JAVADOC_BUILDER;
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
        .addMethod(constructor(analysisResult))
        .addFields(presentInstances(of(threadLocalField(analysisResult.config))))
        .addMethod(builderMethod(analysisResult))
        .addMethods(presentInstances(of(toBuilderMethod(analysisResult))))
        .addAnnotations(generatedAnnotations(elements))
        .addModifiers(toArray(maybeAddPublic(analysisResult.config.isPublic, FINAL), Modifier.class))
        .addType(analysisResult.context.builderImpl())
        .build();
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

  private Optional<MethodSpec> toBuilderMethod(AnalysisResult analysisResult) {
    if (!analysisResult.context.innerContext.toBuilder) {
      return absent();
    }
    String parameterName = downcase(analysisResult.context.innerContext.goalTypeName());
    MethodSpec.Builder builder = methodBuilder("toBuilder")
        .addParameter(analysisResult.context.innerContext.goalType, parameterName);
    String varUpdater = "updater";
    ClassName updaterType = analysisResult.context.updaterContext.typeName();
    if (analysisResult.config.nogc) {
      builder.addStatement("$T $L = $L.get().$N", updaterType, varUpdater,
          STATIC_FIELD_INSTANCE, updaterField(analysisResult.context));
    } else {
      builder.addStatement("$T $L = new $T()", updaterType, varUpdater,
          updaterType);
    }
    for (ParameterContext stepSpec : analysisResult.context.innerContext.stepSpecs) {
      if (stepSpec.projectionMethodName.isPresent()) {
        builder.addStatement("$N.$N = $N.$N()", varUpdater, stepSpec.parameter.getSimpleName(),
            parameterName, stepSpec.projectionMethodName.get());
      } else {
        builder.addStatement("$N.$N = $N.$N", varUpdater, stepSpec.parameter.getSimpleName(),
            parameterName, stepSpec.parameter.getSimpleName());
      }
    }
    builder.addStatement("return $L", varUpdater);
    return Optional.of(builder
        .returns(analysisResult.context.innerContext.contractUpdaterName())
        .addModifiers(analysisResult.context.innerContext.maybeAddPublic(STATIC)).build());
  }

  private MethodSpec builderMethod(AnalysisResult analysisResult) {
    ParameterContext firstStep = analysisResult.context.innerContext.stepSpecs.get(0);
    Optional<ClassName> maybeReceiver = analysisResult.context.innerContext.receiverType();
    MethodSpec.Builder builder = methodBuilder(
        downcase(analysisResult.context.innerContext.goalName() + "Builder"));
    if (maybeReceiver.isPresent()) {
      ClassName receiver = maybeReceiver.get();
      builder.addParameter(ParameterSpec.builder(receiver,
          downcase(receiver.simpleName())).build());
      if (analysisResult.config.nogc) {
        builder.addStatement("$T $N = $N.get().$N", analysisResult.context.innerContext.stepsImplTypeName(),
            downcase(analysisResult.context.innerContext.stepsImplTypeName().simpleName()), STATIC_FIELD_INSTANCE,
            stepsField(analysisResult.context));
      } else {
        builder.addStatement("$T $N = new $T()", analysisResult.context.innerContext.stepsImplTypeName(),
            downcase(analysisResult.context.innerContext.stepsImplTypeName().simpleName()),
            analysisResult.context.innerContext.stepsImplTypeName());
      }
      builder.addStatement("$N.$N = $N",
          downcase(analysisResult.context.innerContext.stepsImplTypeName().simpleName()),
          "_" + downcase(receiver.simpleName()),
          downcase(receiver.simpleName()));
      builder.addStatement("return $N",
          downcase(analysisResult.context.innerContext.stepsImplTypeName().simpleName()));
    } else {
      if (analysisResult.config.nogc) {
        builder.addStatement("return $N.get().$N", STATIC_FIELD_INSTANCE,
            stepsField(analysisResult.context));
      } else {
        builder.addStatement("return new $T()", analysisResult.context.innerContext.stepsImplTypeName());
      }
    }
    return builder.returns(firstStep.stepContractType)
        .addJavadoc(JAVADOC_BUILDER, analysisResult.context.innerContext.goalType)
        .addModifiers(analysisResult.context.innerContext.maybeAddPublic(STATIC))
        .build();
  }

  private ImmutableList<FieldSpec> instanceFields(AnalysisResult analysisResult) {
    if (!analysisResult.config.nogc) {
      return ImmutableList.of();
    }
    ImmutableList.Builder<FieldSpec> builder = ImmutableList.builder();
    if (analysisResult.context.innerContext.toBuilder) {
      builder.add(FieldSpec.builder(analysisResult.context.updaterContext.typeName(),
          downcase(analysisResult.context.innerContext.goalName() + "Updater"), PRIVATE, FINAL).build());
    }
    builder.add(FieldSpec.builder(analysisResult.context.innerContext.stepsImplTypeName(),
        downcase(analysisResult.context.innerContext.goalName() + "Steps"), PRIVATE, FINAL).build());
    return builder.build();
  }

  private MethodSpec constructor(AnalysisResult analysisResult) {
    MethodSpec.Builder builder = constructorBuilder();
    if (analysisResult.config.nogc && analysisResult.context.innerContext.toBuilder) {
      builder.addStatement("this.$L = new $T()",
          updaterField(analysisResult.context), analysisResult.context.updaterContext.typeName());
    }
    if (analysisResult.config.nogc) {
      builder.addStatement("this.$L = new $T()",
          stepsField(analysisResult.context), analysisResult.context.innerContext.stepsImplTypeName());
    }
    return builder.addModifiers(PRIVATE).build();
  }

  private String updaterField(GoalContext context) {
    return downcase(context.innerContext.goalName() + "Updater");
  }

  private String stepsField(GoalContext context) {
    return downcase(context.innerContext.goalName() + "Steps");
  }

}
