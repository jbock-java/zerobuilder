package net.zerobuilder.compiler;

import com.google.common.base.Optional;
import com.squareup.javapoet.ClassName;
import com.squareup.javapoet.CodeBlock;
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
import static javax.lang.model.element.Modifier.STATIC;
import static net.zerobuilder.compiler.Messages.JavadocMessages.JAVADOC_BUILDER;
import static net.zerobuilder.compiler.Messages.JavadocMessages.generatedAnnotations;
import static net.zerobuilder.compiler.Util.downcase;
import static net.zerobuilder.compiler.Util.joinCodeBlocks;
import static net.zerobuilder.compiler.Util.upcase;

final class MyGenerator extends SourceFileGenerator<MyContext> {

  private static final String STATIC_FIELD_INSTANCE = "INSTANCE";
  private static final String FIELD_UPDATER = "updater";
  private static final String FIELD_STEPS = "steps";

  private final Elements elements;

  MyGenerator(Filer filer, Elements elements) {
    super(filer);
    this.elements = elements;
  }

  @Override
  TypeSpec write(
      ClassName generatedClassName, MyContext context) {
    return classBuilder(generatedClassName)
        .addFields(presentInstances(of(updaterField(context))))
        .addField(context.stepsImplTypeName(), "steps", PRIVATE, FINAL)
        .addMethod(constructor(context))
        .addField(threadLocalField(context.generatedTypeName()))
        .addMethod(builderMethod(context))
        .addMethods(presentInstances(of(toBuilderMethod(context))))
        .addTypes(presentInstances(of(context.updaterContext().buildUpdaterImpl())))
        .addType(context.stepsContext().buildStepsImpl())
        .addType(context.contractContext().buildContract())
        .addAnnotations(generatedAnnotations(elements))
        .addModifiers(toArray(context.maybeAddPublic(FINAL), Modifier.class))
        .build();
  }

  private Optional<FieldSpec> updaterField(MyContext context) {
    if (!context.toBuilder()) {
      return absent();
    }
    return Optional.of(FieldSpec.builder(context.updaterContext().typeName(),
        "updater", PRIVATE, FINAL).build());
  }

  private static MethodSpec constructor(MyContext context) {
    return constructorBuilder()
        .addCode(joinCodeBlocks(presentInstances(of(constructorUpdaterStatement(context))), ""))
        .addStatement("this.$L = new $T()", FIELD_STEPS, context.stepsImplTypeName())
        .addModifiers(PRIVATE)
        .build();
  }

  private static Optional<CodeBlock> constructorUpdaterStatement(MyContext context) {
    if (!context.toBuilder()) {
      return absent();
    }
    return Optional.of(CodeBlock.of("this.$L = new $T();", FIELD_UPDATER, context.updaterContext().typeName()));
  }

  static FieldSpec threadLocalField(ClassName generatedType) {
    TypeName threadLocal = ParameterizedTypeName.get(ClassName.get(ThreadLocal.class), generatedType);
    MethodSpec initialValue = methodBuilder("initialValue")
        .addAnnotation(Override.class)
        .addModifiers(PROTECTED)
        .returns(generatedType)
        .addStatement("return new $T()", generatedType)
        .build();
    return FieldSpec.builder(threadLocal, STATIC_FIELD_INSTANCE)
        .initializer("$L", anonymousClassBuilder("")
            .addSuperinterface(threadLocal)
            .addMethod(initialValue)
            .build())
        .addModifiers(PRIVATE, STATIC, FINAL)
        .build();
  }

  private Optional<MethodSpec> toBuilderMethod(MyContext context) {
    if (!context.toBuilder()) {
      return absent();
    }
    String parameterName = downcase(ClassName.get(context.buildElement).simpleName());
    MethodSpec.Builder builder = methodBuilder("toBuilder")
        .addParameter(context.goalType, parameterName);
    String varUpdater = "updater";
    builder.addStatement("$T $L = $L.get().$N", context.updaterContext().typeName(), varUpdater, STATIC_FIELD_INSTANCE, FIELD_UPDATER);
    for (StepSpec stepSpec : context.stepSpecs) {
      switch (context.accessType) {
        case AUTOVALUE:
          builder.addStatement("$N.$N = $N.$N()", varUpdater, stepSpec.argument.getSimpleName(),
              parameterName, stepSpec.argument.getSimpleName());
          break;
        case FIELDS:
          builder.addStatement("$N.$N = $N.$N", varUpdater, stepSpec.argument.getSimpleName(),
              parameterName, stepSpec.argument.getSimpleName());
          break;
        case GETTERS:
          builder.addStatement("$N.$N = $N.$N()", varUpdater, stepSpec.argument.getSimpleName(),
              parameterName, "get" + upcase(stepSpec.argument.getSimpleName().toString()));
          break;
        default:
      }
    }
    builder.addStatement("return $L", varUpdater);
    return Optional.of(builder
        .returns(context.contractUpdaterName())
        .addModifiers(context.maybeAddPublic(STATIC)).build());
  }

  private MethodSpec builderMethod(MyContext context) {
    StepSpec firstStep = context.stepSpecs.get(0);
    Optional<ClassName> receiver = context.receiver();
    MethodSpec.Builder builder = methodBuilder("builder");
    if (receiver.isPresent()) {
      ClassName r = receiver.get();
      builder.addParameter(ParameterSpec.builder(r, downcase(r.simpleName())).build());
      builder.addStatement("$T $N = $N.get().$N", context.stepsImplTypeName(),
          downcase(context.stepsImplTypeName().simpleName()), STATIC_FIELD_INSTANCE, FIELD_STEPS);
      builder.addStatement("$N.$N = $N", downcase(context.stepsImplTypeName().simpleName()),
          "_" + downcase(r.simpleName()), downcase(r.simpleName()));
      builder.addStatement("return $N", downcase(context.stepsImplTypeName().simpleName()));
    } else {
      builder.addStatement("return $N.get().$N", STATIC_FIELD_INSTANCE, FIELD_STEPS);
    }
    return builder.returns(firstStep.stepName)
        .addJavadoc(JAVADOC_BUILDER, ClassName.get(context.buildElement))
        .addModifiers(context.maybeAddPublic(STATIC))
        .build();
  }

}
