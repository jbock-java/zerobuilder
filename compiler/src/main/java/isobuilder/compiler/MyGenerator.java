package isobuilder.compiler;

import com.google.common.base.Joiner;
import com.google.common.base.Optional;
import com.google.common.collect.ImmutableList;
import com.squareup.javapoet.ClassName;
import com.squareup.javapoet.MethodSpec;
import com.squareup.javapoet.ParameterSpec;
import com.squareup.javapoet.TypeSpec;
import isobuilder.Builder;

import javax.annotation.processing.Filer;
import javax.lang.model.element.Element;
import javax.lang.model.util.Elements;

import static com.squareup.javapoet.MethodSpec.constructorBuilder;
import static com.squareup.javapoet.MethodSpec.methodBuilder;
import static com.squareup.javapoet.TypeSpec.classBuilder;
import static javax.lang.model.element.Modifier.FINAL;
import static javax.lang.model.element.Modifier.PRIVATE;
import static javax.lang.model.element.Modifier.PUBLIC;
import static javax.lang.model.element.Modifier.STATIC;

final class MyGenerator extends SourceFileGenerator<Target> {

  MyGenerator(Filer filer, Elements elements) {
    super(filer, elements);
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
    StepSpec firstStep = target.stepSpecs.get(0);
    StepSpec secondStep = target.stepSpecs.get(1);
    ParameterSpec parameter = firstStep.asParameter();
    MethodSpec builderFactory = methodBuilder(firstStep.argument.getSimpleName().toString())
        .returns(secondStep.stepName)
        .addParameter(parameter)
        .addJavadoc(Joiner.on('\n').join(ImmutableList.of(
            "First step of the builder chain that generates {@link $T}.",
            "",
            "<h1>Warning</h1>",
            "As of Java 8, aliasing this method in the {@link $T} annotated class",
            "may cause a compile error, due to a limitation of javac annotation processing.")),
            target.goalType(), Builder.class)
        .addStatement("return new $T($N)", target.implName(), parameter.name)
        .addModifiers(PUBLIC, STATIC)
        .build();
    return Optional.of(classBuilder(generatedClassName)
        .addMethod(builderFactory)
        .addType(buildImpl(target))
        .addType(buildContract(target))
        .addModifiers(PUBLIC, FINAL)
        .addMethod(constructorBuilder().addModifiers(PRIVATE).build()));
  }

  private static TypeSpec buildImpl(Target target) {
    Target.Impl impl = target.impl();
    return classBuilder(target.implName())
        .addSuperinterfaces(target.contractInterfaces())
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
