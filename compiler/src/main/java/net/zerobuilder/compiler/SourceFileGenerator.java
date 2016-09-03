package net.zerobuilder.compiler;

import com.squareup.javapoet.JavaFile;
import com.squareup.javapoet.TypeSpec;

import javax.annotation.processing.Filer;
import javax.lang.model.element.Element;
import javax.tools.JavaFileObject;
import java.io.IOException;
import java.io.Writer;

import static com.google.common.base.Preconditions.checkNotNull;
import static com.google.common.base.Throwables.propagate;
import static com.google.common.collect.Iterables.toArray;

abstract class SourceFileGenerator {

  private final Filer filer;

  SourceFileGenerator(Filer filer) {
    this.filer = checkNotNull(filer);
  }

  final void generate(BuildConfig config, GoalContext context) {
    try {
      TypeSpec type = write(config, context);
      JavaFile javaFile = JavaFile.builder(config.generatedType.packageName(), type)
          .skipJavaLangImports(true)
          .build();
      JavaFileObject sourceFile = filer.createSourceFile(
          config.generatedType.toString(),
          toArray(javaFile.typeSpec.originatingElements, Element.class));
      try (Writer writer = sourceFile.openWriter()) {
        writer.write(javaFile.toString());
      } catch (IOException e) {
        String message = "Could not write generated class " + config.generatedType + ": " + e;
        throw new RuntimeException(message);
      }
    } catch (Exception e) {
      propagate(e);
    }
  }

  abstract TypeSpec write(BuildConfig config, GoalContext context);
}
