package net.zerobuilder.compiler;

import com.squareup.javapoet.ClassName;
import com.squareup.javapoet.JavaFile;
import com.squareup.javapoet.TypeSpec;

import javax.annotation.processing.Filer;
import javax.lang.model.element.Element;
import javax.tools.JavaFileObject;
import java.io.IOException;
import java.io.Writer;

import static com.google.common.base.Preconditions.checkNotNull;
import static com.google.common.base.Throwables.propagateIfPossible;
import static com.google.common.collect.Iterables.toArray;

abstract class SourceFileGenerator<T extends GenerationContext> {

  private final Filer filer;

  SourceFileGenerator(Filer filer) {
    this.filer = checkNotNull(filer);
  }

  final void generate(T input) {
    ClassName generatedTypeName = input.generatedTypeName();
    try {
      TypeSpec type = write(generatedTypeName, input);
      JavaFile javaFile = JavaFile.builder(generatedTypeName.packageName(), type)
          .skipJavaLangImports(true)
          .build();
      JavaFileObject sourceFile = filer.createSourceFile(
          generatedTypeName.toString(),
          toArray(javaFile.typeSpec.originatingElements, Element.class));
      try (Writer writer = sourceFile.openWriter()) {
        writer.write(javaFile.toString());
      } catch (IOException e) {
        String message = "Could not write generated class " + generatedTypeName + ": " + e;
        throw new RuntimeException(message);
      }
    } catch (Exception e) {
      propagateIfPossible(e);
      throw new RuntimeException(e);
    }
  }

  abstract TypeSpec write(ClassName generatedTypeName, T input);
}
