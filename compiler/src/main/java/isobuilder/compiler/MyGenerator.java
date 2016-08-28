package isobuilder.compiler;

import com.google.common.base.Optional;
import com.squareup.javapoet.ClassName;
import com.squareup.javapoet.FieldSpec;
import com.squareup.javapoet.MethodSpec;
import com.squareup.javapoet.ParameterizedTypeName;
import com.squareup.javapoet.TypeName;
import com.squareup.javapoet.TypeSpec;

import javax.annotation.processing.Filer;
import javax.annotation.processing.Messager;
import javax.lang.model.element.Element;
import javax.lang.model.util.Elements;
import java.util.EnumSet;

import static com.squareup.javapoet.MethodSpec.constructorBuilder;
import static com.squareup.javapoet.MethodSpec.methodBuilder;
import static com.squareup.javapoet.TypeSpec.anonymousClassBuilder;
import static com.squareup.javapoet.TypeSpec.classBuilder;
import static isobuilder.compiler.ErrorMessages.INFO_BUILDER_JAVADOC;
import static isobuilder.compiler.Util.downcase;
import static javax.lang.model.element.Modifier.FINAL;
import static javax.lang.model.element.Modifier.PRIVATE;
import static javax.lang.model.element.Modifier.PROTECTED;
import static javax.lang.model.element.Modifier.STATIC;

final class MyGenerator extends SourceFileGenerator<Target> {

  private static final String INSTANCE = "INSTANCE";

  MyGenerator(Filer filer, Elements elements, Messager messager) {
    super(filer, elements, messager);
  }

  @Override
  ClassName nameGeneratedType(Target target) {
    return target.generatedClassName();
  }

  @Override
  Optional<? extends Element> getElementForErrorReporting(Target input) {
    return Optional.absent();
  }

  @Override
  Optional<TypeSpec.Builder> write(
      ClassName generatedClassName, Target target) {
    return Optional.of(classBuilder(generatedClassName)
        .addField(target.updaterImpl().name(), "updater", PRIVATE, FINAL)
        .addField(target.stepsImpl().name(), "steps", PRIVATE, FINAL)
        .addMethod(constructor(target))
        .addField(threadLocalField(target.generatedClassName()))
        .addMethod(builderMethod(target))
        .addMethod(toBuilderMethod(target))
        .addType(buildUpdaterImpl(target.contractUpdaterName(), target.updaterImpl()))
        .addType(buildStepsImpl(target.contract(), target.stepsImpl()))
        .addType(buildContract(target))
        .addModifiers(target.typeModifiers(EnumSet.of(FINAL))));
  }

  private MethodSpec constructor(Target target) {
    return constructorBuilder()
        .addStatement("this.$L = new $T()", "updater", target.updaterImpl().name())
        .addStatement("this.$L = new $T()", "steps", target.stepsImpl().name())
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
    return FieldSpec.builder(threadLocal, INSTANCE)
        .initializer("$L", anonymousClassBuilder("")
            .addSuperinterface(threadLocal)
            .addMethod(initialValue)
            .build())
        .addModifiers(PRIVATE, STATIC, FINAL)
        .build();
  }

  private MethodSpec toBuilderMethod(Target target) {
    String parameterName = downcase(ClassName.get(target.typeElement).simpleName());
    MethodSpec.Builder builder = methodBuilder("toBuilder")
        .addParameter(ClassName.get(target.typeElement), parameterName);
    String updater = "updater";
    builder.addStatement("$T $L = $L.get().updater", target.updaterImpl().name(), updater, INSTANCE);
    for (StepSpec stepSpec : target.stepSpecs) {
      // support getters, DFA
      builder.addStatement("$L.$L($N.$L())", updater, stepSpec.argument.getSimpleName(),
          parameterName, stepSpec.argument.getSimpleName());
    }
    builder.addStatement("return $L", updater);
    return builder
        .returns(target.contractUpdaterName())
        .addModifiers(target.methodModifiers(EnumSet.of(STATIC))).build();
  }

  private MethodSpec builderMethod(Target target) {
    StepSpec firstStep = target.stepSpecs.get(0);
    return methodBuilder("builder")
        .returns(firstStep.stepName)
        .addJavadoc(INFO_BUILDER_JAVADOC, ClassName.get(target.typeElement))
        .addStatement("return $N.get().steps", INSTANCE)
        .addModifiers(target.methodModifiers(EnumSet.of(STATIC)))
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
        .addModifiers(target.typeModifiers(EnumSet.of(FINAL, STATIC)))
        .addMethod(constructorBuilder().addModifiers(PRIVATE).build())
        .build();
  }

}
