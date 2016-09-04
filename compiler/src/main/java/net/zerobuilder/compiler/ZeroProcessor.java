package net.zerobuilder.compiler;

import com.google.auto.service.AutoService;
import com.google.common.base.Function;
import com.google.common.collect.FluentIterable;
import com.google.common.collect.ImmutableSet;
import com.squareup.javapoet.ClassName;
import com.squareup.javapoet.JavaFile;
import com.squareup.javapoet.TypeSpec;
import net.zerobuilder.Build;
import net.zerobuilder.Goal;

import javax.annotation.Generated;
import javax.annotation.processing.AbstractProcessor;
import javax.annotation.processing.Messager;
import javax.annotation.processing.Processor;
import javax.annotation.processing.RoundEnvironment;
import javax.lang.model.SourceVersion;
import javax.lang.model.element.Element;
import javax.lang.model.element.ExecutableElement;
import javax.lang.model.element.TypeElement;
import javax.lang.model.util.Elements;
import javax.tools.Diagnostic;
import javax.tools.JavaFileObject;
import java.io.IOException;
import java.io.Writer;
import java.util.Set;

import static com.google.common.base.Throwables.getStackTraceAsString;
import static com.google.common.base.Throwables.propagate;
import static com.google.common.collect.Iterables.toArray;
import static com.google.common.collect.Sets.union;
import static javax.lang.model.util.ElementFilter.constructorsIn;
import static javax.lang.model.util.ElementFilter.methodsIn;
import static javax.lang.model.util.ElementFilter.typesIn;
import static javax.tools.Diagnostic.Kind.ERROR;

@AutoService(Processor.class)
public final class ZeroProcessor extends AbstractProcessor {

  @Override
  public Set<String> getSupportedAnnotationTypes() {
    return ImmutableSet.of(Goal.class.getName(), Build.class.getName());
  }

  @Override
  public SourceVersion getSupportedSourceVersion() {
    return SourceVersion.latestSupported();
  }

  @Override
  public boolean process(Set<? extends TypeElement> annotations, RoundEnvironment env) {
    Messager messager = processingEnv.getMessager();
    if (!allGoalsEnclosed(messager, env)) {
      return false;
    }
    Elements elements = processingEnv.getElementUtils();
    Analyser transformer = new Analyser(elements);
    Generator generator = new Generator(elements);
    Set<TypeElement> types = typesIn(env.getElementsAnnotatedWith(Build.class));
    for (TypeElement annotatedType : types) {
      try {
        Analyser.AnalysisResult analysisResult = transformer.parse(annotatedType);
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

  private boolean allGoalsEnclosed(Messager messager, RoundEnvironment env) {
    Set<? extends Element> elements = env.getElementsAnnotatedWith(Goal.class);
    Set<ExecutableElement> executableElements =
        ImmutableSet.copyOf(union(constructorsIn(elements), methodsIn(elements)));
    for (ExecutableElement executableElement : executableElements) {
      if (executableElement.getEnclosingElement().getAnnotation(Build.class) == null) {
        messager.printMessage(ERROR, Messages.ErrorMessages.GOAL_NOT_IN_BUILD, executableElement);
        return false;
      }
    }
    return true;
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