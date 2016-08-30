package net.zerobuilder.compiler;

import com.squareup.javapoet.ClassName;
import com.squareup.javapoet.FieldSpec;
import com.squareup.javapoet.MethodSpec;
import com.squareup.javapoet.ParameterizedTypeName;
import com.squareup.javapoet.TypeName;
import com.squareup.javapoet.TypeSpec;

import javax.annotation.processing.Filer;
import javax.lang.model.element.Modifier;
import javax.lang.model.util.Elements;

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
        .addField(context.updaterContext().name(), "updater", PRIVATE, FINAL)
        .addField(context.stepsContext().name(), "steps", PRIVATE, FINAL)
        .addMethod(constructor(context))
        .addField(threadLocalField(context.generatedTypeName()))
        .addMethod(builderMethod(context))
        .addMethod(toBuilderMethod(context))
        .addType(buildUpdaterImpl(context.contractUpdaterName(), context.updaterContext()))
        .addType(buildStepsImpl(context.contractContext(), context.stepsContext()))
        .addType(buildContract(context))
        .addAnnotations(generatedAnnotations(elements))
        .addModifiers(toArray(context.maybeAddPublic(FINAL), Modifier.class))
        .build();
  }

  private MethodSpec constructor(MyContext context) {
    return constructorBuilder()
        .addStatement("this.$L = new $T()", FIELD_UPDATER, context.updaterContext().name())
        .addStatement("this.$L = new $T()", FIELD_STEPS, context.stepsContext().name())
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

  private MethodSpec toBuilderMethod(MyContext context) {
    String parameterName = downcase(ClassName.get(context.buildElement).simpleName());
    MethodSpec.Builder builder = methodBuilder("toBuilder")
        .addParameter(ClassName.get(context.buildElement), parameterName);
    String varUpdater = "updater";
    builder.addStatement("$T $L = $L.get().$N", context.updaterContext().name(), varUpdater, STATIC_FIELD_INSTANCE, FIELD_UPDATER);
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
    return builder
        .returns(context.contractUpdaterName())
        .addModifiers(context.maybeAddPublic(STATIC)).build();
  }

  private MethodSpec builderMethod(MyContext context) {
    StepSpec firstStep = context.stepSpecs.get(0);
    return methodBuilder("builder")
        .returns(firstStep.stepName)
        .addJavadoc(JAVADOC_BUILDER, ClassName.get(context.buildElement))
        .addStatement("return $N.get().$N", STATIC_FIELD_INSTANCE, FIELD_STEPS)
        .addModifiers(context.maybeAddPublic(STATIC))
        .build();
  }

  private static TypeSpec buildStepsImpl(ContractContext contract, StepsContext impl) {
    return classBuilder(impl.name())
        .addSuperinterfaces(contract.stepInterfaceNames())
        .addFields(impl.fields())
        .addMethod(impl.constructor())
        .addMethods(impl.stepsButLast())
        .addMethod(impl.lastStep())
        .addModifiers(FINAL, STATIC)
        .build();
  }

  private static TypeSpec buildUpdaterImpl(ClassName updateType, UpdaterContext impl) {
    return classBuilder(impl.name())
        .addSuperinterface(updateType)
        .addFields(impl.fields())
        .addMethod(impl.constructor())
        .addMethods(impl.updaterMethods())
        .addMethod(impl.buildMethod())
        .addModifiers(FINAL, STATIC)
        .build();
  }

  private static TypeSpec buildContract(MyContext context) {
    ContractContext contract = context.contractContext();
    return classBuilder(context.contractName())
        .addType(contract.updaterInterface())
        .addTypes(contract.stepInterfaces())
        .addModifiers(toArray(context.maybeAddPublic(FINAL, STATIC), Modifier.class))
        .addMethod(constructorBuilder().addModifiers(PRIVATE).build())
        .build();
  }

}
