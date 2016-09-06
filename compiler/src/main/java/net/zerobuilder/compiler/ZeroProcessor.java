package net.zerobuilder.compiler;

import com.google.auto.service.AutoService;
import com.google.common.base.Optional;
import com.google.common.collect.ImmutableSet;
import com.squareup.javapoet.ClassName;
import com.squareup.javapoet.JavaFile;
import com.squareup.javapoet.TypeSpec;
import net.zerobuilder.Build;
import net.zerobuilder.Goal;
import net.zerobuilder.compiler.Analyser.AnalysisResult;

import javax.annotation.processing.AbstractProcessor;
import javax.annotation.processing.Processor;
import javax.annotation.processing.RoundEnvironment;
import javax.lang.model.SourceVersion;
import javax.lang.model.element.Element;
import javax.lang.model.element.ExecutableElement;
import javax.lang.model.element.TypeElement;
import javax.tools.JavaFileObject;
import java.io.IOException;
import java.io.Writer;
import java.util.Set;

import static com.google.common.base.Throwables.getStackTraceAsString;
import static com.google.common.collect.Iterables.toArray;
import static com.google.common.collect.Sets.union;
import static javax.lang.model.util.ElementFilter.constructorsIn;
import static javax.lang.model.util.ElementFilter.methodsIn;
import static javax.lang.model.util.ElementFilter.typesIn;
import static javax.tools.Diagnostic.Kind.ERROR;
import static net.zerobuilder.compiler.Messages.ErrorMessages.GOAL_NOT_IN_BUILD;

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
    Optional<ExecutableElement> goalNotInBuild = goalNotInBuild(env);
    if (goalNotInBuild.isPresent()) {
      processingEnv.getMessager().printMessage(ERROR,
          GOAL_NOT_IN_BUILD, goalNotInBuild.get());
      return false;
    }
    Analyser transformer = new Analyser(processingEnv.getElementUtils());
    Generator generator = new Generator(processingEnv.getElementUtils());
    Set<TypeElement> types = typesIn(env.getElementsAnnotatedWith(Build.class));
    for (TypeElement annotatedType : types) {
      try {
        AnalysisResult analysisResult = transformer.parse(annotatedType);
        TypeSpec typeSpec = generator.generate(analysisResult);
        try {
          write(analysisResult.config.generatedType, typeSpec);
        } catch (IOException e) {
          String message = "Error processing "
              + ClassName.get(annotatedType) + ": " + getStackTraceAsString(e);
          processingEnv.getMessager().printMessage(ERROR, message, annotatedType);
          return false;
        }
      } catch (ValidationException e) {
        processingEnv.getMessager().printMessage(e.kind, e.getMessage(), e.about);
      } catch (RuntimeException e) {
        String message = "Error processing "
            + ClassName.get(annotatedType) + ": " + getStackTraceAsString(e);
        processingEnv.getMessager().printMessage(ERROR, message, annotatedType);
        return false;
      }
    }
    return false;
  }

  private Optional<ExecutableElement> goalNotInBuild(RoundEnvironment env) {
    Set<? extends Element> elements = env.getElementsAnnotatedWith(Goal.class);
    Set<ExecutableElement> executableElements =
        ImmutableSet.copyOf(union(constructorsIn(elements), methodsIn(elements)));
    for (ExecutableElement executableElement : executableElements) {
      if (executableElement.getEnclosingElement().getAnnotation(Build.class) == null) {
        return Optional.of(executableElement);
      }
    }
    return Optional.absent();
  }

  private void write(ClassName generatedType, TypeSpec typeSpec) throws IOException {
    JavaFile javaFile = JavaFile.builder(generatedType.packageName(), typeSpec)
        .skipJavaLangImports(true)
        .build();
    JavaFileObject sourceFile = processingEnv.getFiler()
        .createSourceFile(generatedType.toString(),
            toArray(javaFile.typeSpec.originatingElements, Element.class));
    try (Writer writer = sourceFile.openWriter()) {
      writer.write(javaFile.toString());
    }
  }
}