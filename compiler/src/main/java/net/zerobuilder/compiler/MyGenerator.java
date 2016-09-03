package net.zerobuilder.compiler;

import com.google.common.base.Optional;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.Iterables;
import com.squareup.javapoet.ClassName;
import com.squareup.javapoet.FieldSpec;
import com.squareup.javapoet.MethodSpec;
import com.squareup.javapoet.ParameterSpec;
import com.squareup.javapoet.ParameterizedTypeName;
import com.squareup.javapoet.TypeName;
import com.squareup.javapoet.TypeSpec;

import javax.annotation.processing.Filer;
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
import static javax.lang.model.element.Modifier.PUBLIC;
import static javax.lang.model.element.Modifier.STATIC;
import static net.zerobuilder.compiler.GoalContext.maybeAddPublic;
import static net.zerobuilder.compiler.Messages.JavadocMessages.JAVADOC_BUILDER;
import static net.zerobuilder.compiler.Messages.JavadocMessages.generatedAnnotations;
import static net.zerobuilder.compiler.Util.downcase;

final class MyGenerator extends SourceFileGenerator {

  private final Elements elements;

  private static final String STATIC_FIELD_INSTANCE = "INSTANCE";

  MyGenerator(Filer filer, Elements elements) {
    super(filer);
    this.elements = elements;
  }

  @Override
  TypeSpec write(BuildConfig config, GoalContext context) {
    return classBuilder(config.generatedType)
        .addFields(instanceFields(config, context))
        .addMethod(constructor(config, context))
        .addFields(presentInstances(of(threadLocalField(config))))
        .addMethod(builderMethod(config, context))
        .addMethods(presentInstances(of(toBuilderMethod(config, context))))
        .addAnnotations(generatedAnnotations(elements))
        .addModifiers(toArray(maybeAddPublic(config.isPublic, FINAL), Modifier.class))
        .addType(context.builderImpl())
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

  private Optional<MethodSpec> toBuilderMethod(BuildConfig config, GoalContext context) {
    if (!config.toBuilder) {
      return absent();
    }
    String parameterName = downcase(context.goalTypeSimpleName());
    MethodSpec.Builder builder = methodBuilder("toBuilder")
        .addParameter(context.goalType, parameterName);
    String varUpdater = "updater";
    ClassName updaterType = context.updaterContext().typeName();
    if (config.nogc) {
      builder.addStatement("$T $L = $L.get().$N", updaterType, varUpdater,
          STATIC_FIELD_INSTANCE, updaterField(context));
    } else {
      builder.addStatement("$T $L = new $T()", updaterType, varUpdater,
          updaterType);
    }
    for (StepSpec stepSpec : context.stepSpecs) {
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
        .returns(context.contractUpdaterName())
        .addModifiers(context.maybeAddPublic(STATIC)).build());
  }

  private MethodSpec builderMethod(BuildConfig config, GoalContext context) {
    StepSpec firstStep = context.stepSpecs.get(0);
    Optional<ClassName> maybeReceiver = context.receiverType();
    MethodSpec.Builder builder = methodBuilder(
        downcase(context.goalTypeSimpleName() + "Builder"));
    if (maybeReceiver.isPresent()) {
      ClassName receiver = maybeReceiver.get();
      builder.addParameter(ParameterSpec.builder(receiver,
          downcase(receiver.simpleName())).build());
      if (config.nogc) {
        builder.addStatement("$T $N = $N.get().$N", context.stepsImplTypeName(),
            downcase(context.stepsImplTypeName().simpleName()), STATIC_FIELD_INSTANCE,
            stepsField(context));
      } else {
        builder.addStatement("$T $N = new $T()", context.stepsImplTypeName(),
            downcase(context.stepsImplTypeName().simpleName()), context.stepsImplTypeName());
      }
      builder.addStatement("$N.$N = $N",
          downcase(context.stepsImplTypeName().simpleName()),
          "_" + downcase(receiver.simpleName()),
          downcase(receiver.simpleName()));
      builder.addStatement("return $N",
          downcase(context.stepsImplTypeName().simpleName()));
    } else {
      if (config.nogc) {
        builder.addStatement("return $N.get().$N", STATIC_FIELD_INSTANCE,
            stepsField(context));
      } else {
        builder.addStatement("return new $T()", context.stepsImplTypeName());
      }
    }
    return builder.returns(firstStep.stepContractType)
        .addJavadoc(JAVADOC_BUILDER, context.goalType)
        .addModifiers(context.maybeAddPublic(STATIC))
        .build();
  }

  private ImmutableList<FieldSpec> instanceFields(BuildConfig config, GoalContext context) {
    if (!config.nogc) {
      return ImmutableList.of();
    }
    ImmutableList.Builder<FieldSpec> builder = ImmutableList.builder();
    if (config.toBuilder) {
      builder.add(FieldSpec.builder(context.updaterContext().typeName(),
          downcase(context.goalTypeSimpleName() + "Updater"), PRIVATE, FINAL).build());
    }
    builder.add(FieldSpec.builder(context.stepsImplTypeName(),
        downcase(context.goalTypeSimpleName() + "Steps"), PRIVATE, FINAL).build());
    return builder.build();
  }

  private MethodSpec constructor(BuildConfig config, GoalContext context) {
    MethodSpec.Builder builder = constructorBuilder();
    if (config.nogc && config.toBuilder) {
      builder.addStatement("this.$L = new $T()",
          updaterField(context), context.updaterContext().typeName());
    }
    if (config.nogc) {
      builder.addStatement("this.$L = new $T()",
          stepsField(context), context.stepsImplTypeName());
    }
    return builder.addModifiers(PRIVATE).build();
  }

  private String updaterField(GoalContext context) {
    return downcase(context.goalTypeSimpleName() + "Updater");
  }

  private String stepsField(GoalContext context) {
    return downcase(context.goalTypeSimpleName() + "Steps");
  }


}
