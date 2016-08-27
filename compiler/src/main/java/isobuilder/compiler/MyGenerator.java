package isobuilder.compiler;

import com.google.common.base.Joiner;
import com.google.common.base.Optional;
import com.google.common.collect.ImmutableList;
import com.squareup.javapoet.ClassName;
import com.squareup.javapoet.FieldSpec;
import com.squareup.javapoet.MethodSpec;
import com.squareup.javapoet.ParameterSpec;
import com.squareup.javapoet.TypeSpec;

import javax.annotation.processing.Filer;
import javax.annotation.processing.Messager;
import javax.lang.model.element.Element;
import javax.lang.model.util.Elements;

import static com.squareup.javapoet.MethodSpec.constructorBuilder;
import static com.squareup.javapoet.MethodSpec.methodBuilder;
import static com.squareup.javapoet.TypeSpec.classBuilder;
import static isobuilder.compiler.Util.downcase;
import static javax.lang.model.element.Modifier.FINAL;
import static javax.lang.model.element.Modifier.PRIVATE;
import static javax.lang.model.element.Modifier.PUBLIC;
import static javax.lang.model.element.Modifier.STATIC;

final class MyGenerator extends SourceFileGenerator<Target> {

  private static final String INSTANCE = "INSTANCE";

  private static final String BUILDER_JAVADOC = Joiner.on('\n').join(ImmutableList.of(
      "The first step of the builder chain that builds {@link $T}.",
      "All steps of the builder implementation are mutable.",
      "It is not recommended to use any of the steps more than once.",
      "@return A mutable builder without any thread safety guarantees.", ""));

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
        .addField(instance(target))
        .addField(target.stepsImpl().name(), "updater", PRIVATE, FINAL)
        .addField(target.updaterImpl().name(), "steps", PRIVATE, FINAL)
        .addMethod(builderMethod(target))
        .addMethod(toBuilderMethod(target))
        .addType(buildImpl(target))
        .addType(buildContract(target))
        .addModifiers(PUBLIC, FINAL)
        .addMethod(constructor());
  }

  private MethodSpec constructor(Target target) {
    return constructorBuilder()
        .addStatement("this.$L = new $N()", "updater", target.stepsImpl().name())
        .addStatement("this.$L = new $N()", "steps", target.stepsImpl().name())
        .addModifiers(PRIVATE)
        .build();
  }

  private FieldSpec instance(Target target) {
    return FieldSpec.builder();
  }

  private MethodSpec toBuilderMethod(Target target) {
    String parameterName = downcase(ClassName.get(target.typeElement).simpleName());
    MethodSpec.Builder builder = methodBuilder("toBuilderMethod")
        .addParameter(ClassName.get(target.typeElement), parameterName);
    String updater = "updater";
    builder.addStatement("$N = $N.get().updater", updater, INSTANCE);
    for (StepSpec stepSpec : target.stepSpecs) {
      // support getters, DFA
      builder.addStatement("$N.$N($T.$N())", updater, stepSpec.argument.getSimpleName(),
          ClassName.get(target.typeElement), stepSpec.argument.getSimpleName());
    }
    builder.addStatement("return $N", updater);
    return builder.addModifiers(PUBLIC, STATIC).build();
  }

  private MethodSpec builderMethod(Target target) {
    StepSpec firstStep = target.stepSpecs.get(0);
    ParameterSpec parameter = firstStep.asParameter();
    return methodBuilder("builder")
        .returns(firstStep.stepName)
        .addJavadoc(BUILDER_JAVADOC, ClassName.get(target.typeElement))
        .addStatement("return $N.get().steps", INSTANCE, parameter.name)
        .addModifiers(PUBLIC, STATIC)
        .build();
  }

  private static TypeSpec buildImpl(Target target) {
    Target.Impl impl = target.impl();
    Target.Contract contract = target.contract();
    return classBuilder(impl.name())
        .addSuperinterfaces(contract.interfaceNames())
        .addFields(impl.fields())
        .addMethod(impl.constructor())
        .addMethods(impl.updaters())
        .addMethods(impl.steppers())
        .addMethod(impl.buildMethod())
        .addModifiers(FINAL, STATIC)
        .build();
  }

  private static TypeSpec buildContract(Target target) {
    Target.Contract contract = target.contract();
    return classBuilder(target.contractName())
        .addTypes(contract.interfaces())
        .addModifiers(PUBLIC, FINAL, STATIC)
        .addMethod(constructorBuilder().addModifiers(PRIVATE).build())
        .build();
  }

}
