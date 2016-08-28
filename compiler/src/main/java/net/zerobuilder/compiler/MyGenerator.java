package net.zerobuilder.compiler;

import com.squareup.javapoet.ClassName;
import com.squareup.javapoet.FieldSpec;
import com.squareup.javapoet.MethodSpec;
import com.squareup.javapoet.ParameterizedTypeName;
import com.squareup.javapoet.TypeName;
import com.squareup.javapoet.TypeSpec;

import javax.annotation.processing.Filer;
import javax.annotation.processing.Messager;
import javax.lang.model.element.Modifier;
import javax.lang.model.util.Elements;

import static com.google.common.collect.Iterables.toArray;
import static com.squareup.javapoet.MethodSpec.constructorBuilder;
import static com.squareup.javapoet.MethodSpec.methodBuilder;
import static com.squareup.javapoet.TypeSpec.anonymousClassBuilder;
import static com.squareup.javapoet.TypeSpec.classBuilder;
import static net.zerobuilder.compiler.Messages.JavadocMessages.JAVADOC_BUILDER;
import static net.zerobuilder.compiler.Messages.JavadocMessages.generatedAnnotations;
import static net.zerobuilder.compiler.Util.downcase;
import static javax.lang.model.element.Modifier.FINAL;
import static javax.lang.model.element.Modifier.PRIVATE;
import static javax.lang.model.element.Modifier.PROTECTED;
import static javax.lang.model.element.Modifier.STATIC;

final class MyGenerator extends SourceFileGenerator<Target> {

  private static final String STATIC_FIELD_INSTANCE = "INSTANCE";
  private static final String FIELD_UPDATER = "updater";
  private static final String FIELD_STEPS = "steps";

  private final Elements elements;

  MyGenerator(Filer filer, Elements elements, Messager messager) {
    super(filer);
    this.elements = elements;
  }

  @Override
  TypeSpec write(
      ClassName generatedClassName, Target target) {
    return classBuilder(generatedClassName)
        .addField(target.updaterImpl().name(), "updater", PRIVATE, FINAL)
        .addField(target.stepsImpl().name(), "steps", PRIVATE, FINAL)
        .addMethod(constructor(target))
        .addField(threadLocalField(target.generatedTypeName()))
        .addMethod(builderMethod(target))
        .addMethod(toBuilderMethod(target))
        .addType(buildUpdaterImpl(target.contractUpdaterName(), target.updaterImpl()))
        .addType(buildStepsImpl(target.contract(), target.stepsImpl()))
        .addType(buildContract(target))
        .addAnnotations(generatedAnnotations(elements))
        .addModifiers(toArray(target.maybeAddPublic(FINAL), Modifier.class))
        .build();
  }

  private MethodSpec constructor(Target target) {
    return constructorBuilder()
        .addStatement("this.$L = new $T()", FIELD_UPDATER, target.updaterImpl().name())
        .addStatement("this.$L = new $T()", FIELD_STEPS, target.stepsImpl().name())
        .addModifiers(PRIVATE)
        .build();
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

  private MethodSpec toBuilderMethod(Target target) {
    String parameterName = downcase(ClassName.get(target.annotatedType).simpleName());
    MethodSpec.Builder builder = methodBuilder("toBuilder")
        .addParameter(ClassName.get(target.annotatedType), parameterName);
    String varUpdater = "updater";
    builder.addStatement("$T $L = $L.get().$N", target.updaterImpl().name(), varUpdater, STATIC_FIELD_INSTANCE, FIELD_UPDATER);
    for (StepSpec stepSpec : target.stepSpecs) {
      // support getters, DFA
      builder.addStatement("$L.$L($N.$L())", varUpdater, stepSpec.argument.getSimpleName(),
          parameterName, stepSpec.argument.getSimpleName());
    }
    builder.addStatement("return $L", varUpdater);
    return builder
        .returns(target.contractUpdaterName())
        .addModifiers(target.maybeAddPublic(STATIC)).build();
  }

  private MethodSpec builderMethod(Target target) {
    StepSpec firstStep = target.stepSpecs.get(0);
    return methodBuilder("builder")
        .returns(firstStep.stepName)
        .addJavadoc(JAVADOC_BUILDER, ClassName.get(target.annotatedType))
        .addStatement("return $N.get().$N", STATIC_FIELD_INSTANCE, FIELD_STEPS)
        .addModifiers(target.maybeAddPublic(STATIC))
        .build();
  }

  private static TypeSpec buildStepsImpl(Contract contract, StepsImpl impl) {
    return classBuilder(impl.name())
        .addSuperinterfaces(contract.stepInterfaceNames())
        .addFields(impl.fields())
        .addMethod(impl.constructor())
        .addMethods(impl.stepsButLast())
        .addMethod(impl.lastStep())
        .addModifiers(FINAL, STATIC)
        .build();
  }

  private static TypeSpec buildUpdaterImpl(ClassName updateType, UpdaterImpl impl) {
    return classBuilder(impl.name())
        .addSuperinterface(updateType)
        .addFields(impl.fields())
        .addMethod(impl.constructor())
        .addMethods(impl.updaterMethods())
        .addMethod(impl.buildMethod())
        .addModifiers(FINAL, STATIC)
        .build();
  }

  private static TypeSpec buildContract(Target target) {
    Contract contract = target.contract();
    return classBuilder(target.contractName())
        .addType(contract.updaterInterface())
        .addTypes(contract.stepInterfaces())
        .addModifiers(toArray(target.maybeAddPublic(FINAL, STATIC), Modifier.class))
        .addMethod(constructorBuilder().addModifiers(PRIVATE).build())
        .build();
  }

}
