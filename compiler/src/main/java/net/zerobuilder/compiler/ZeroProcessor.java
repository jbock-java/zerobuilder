package net.zerobuilder.compiler;

import com.google.auto.service.AutoService;
import com.google.common.collect.ImmutableSet;
import com.squareup.javapoet.ClassName;
import com.squareup.javapoet.JavaFile;
import com.squareup.javapoet.TypeSpec;
import net.zerobuilder.Build;

import javax.annotation.processing.AbstractProcessor;
import javax.annotation.processing.Messager;
import javax.annotation.processing.Processor;
import javax.annotation.processing.RoundEnvironment;
import javax.lang.model.SourceVersion;
import javax.lang.model.element.Element;
import javax.lang.model.element.TypeElement;
import javax.lang.model.util.Elements;
import javax.tools.JavaFileObject;
import java.io.IOException;
import java.io.Writer;
import java.util.Set;

import static com.google.common.base.Throwables.getStackTraceAsString;
import static com.google.common.base.Throwables.propagate;
import static com.google.common.collect.Iterables.toArray;
import static javax.lang.model.util.ElementFilter.typesIn;
import static javax.tools.Diagnostic.Kind.ERROR;

@AutoService(Processor.class)
public final class ZeroProcessor extends AbstractProcessor {

  @Override
  public Set<String> getSupportedAnnotationTypes() {
    return ImmutableSet.of(Build.class.getName());
  }

  @Override
  public SourceVersion getSupportedSourceVersion() {
    return SourceVersion.latestSupported();
  }

  @Override
  public boolean process(Set<? extends TypeElement> annotations, RoundEnvironment env) {
    Elements elements = processingEnv.getElementUtils();
    Messager messager = processingEnv.getMessager();
    Analyzer transformer = new Analyzer(elements);
    Generator generator = new Generator(elements);
    Set<TypeElement> types = typesIn(env.getElementsAnnotatedWith(Build.class));
    for (TypeElement annotatedType : types) {
      try {
        Analyzer.AnalysisResult analysisResult = transformer.parse(annotatedType);
        TypeSpec typeSpec = generator.generate(analysisResult);
        try {
          write(analysisResult.config.generatedType, typeSpec);
        } catch (IOException e) {
          String message = "Could not write generated class "
              + analysisResult.config.generatedType + ": " + getStackTraceAsString(e);
          messager.printMessage(ERROR, message, annotatedType);
          throw new RuntimeException(message);
        }
      } catch (ValidationException e) {
        e.printMessage(messager);
      } catch (RuntimeException e) {
        String message = "Code generation failed: " + getStackTraceAsString(e);
        messager.printMessage(ERROR, message, annotatedType);
        throw propagate(e);
      }
    }
    return false;
  }

  private void write(ClassName generatedType, TypeSpec typeSpec) throws IOException {
    JavaFile javaFile = JavaFile.builder(generatedType.packageName(), typeSpec)
        .skipJavaLangImports(true)
        .build();
    JavaFileObject sourceFile = processingEnv.getFiler().createSourceFile(generatedType.toString(),
        toArray(javaFile.typeSpec.originatingElements, Element.class));
    try (Writer writer = sourceFile.openWriter()) {
      writer.write(javaFile.toString());
    }
  }
}