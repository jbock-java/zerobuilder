package net.zerobuilder.compiler;

import com.squareup.javapoet.ClassName;
import com.squareup.javapoet.TypeSpec;

import javax.annotation.processing.Filer;
import javax.lang.model.element.Modifier;
import javax.lang.model.util.Elements;

import static com.google.common.base.Optional.presentInstances;
import static com.google.common.collect.ImmutableList.of;
import static com.google.common.collect.Iterables.toArray;
import static com.squareup.javapoet.TypeSpec.classBuilder;
import static javax.lang.model.element.Modifier.FINAL;
import static net.zerobuilder.compiler.Messages.JavadocMessages.generatedAnnotations;

final class MyGenerator extends SourceFileGenerator<MyContext> {

  private final Elements elements;

  MyGenerator(Filer filer, Elements elements) {
    super(filer);
    this.elements = elements;
  }

  @Override
  TypeSpec write(
      ClassName generatedClassName, MyContext context) {
    return classBuilder(generatedClassName)
        .addFields(context.instanceFields())
        .addMethod(context.constructor())
        .addFields(presentInstances(of(context.threadLocalField())))
        .addMethod(context.builderMethod())
        .addMethods(presentInstances(of(context.toBuilderMethod())))
        .addTypes(presentInstances(of(context.updaterContext().buildUpdaterImpl())))
        .addType(context.stepsContext().buildStepsImpl())
        .addType(context.contractContext().buildContract())
        .addAnnotations(generatedAnnotations(elements))
        .addModifiers(toArray(context.maybeAddPublic(FINAL), Modifier.class))
        .build();
  }

}
