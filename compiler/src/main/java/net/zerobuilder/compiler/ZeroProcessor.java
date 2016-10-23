package net.zerobuilder.compiler;

import com.squareup.javapoet.AnnotationSpec;
import com.squareup.javapoet.ClassName;
import com.squareup.javapoet.JavaFile;
import com.squareup.javapoet.TypeSpec;
import net.zerobuilder.Builders;
import net.zerobuilder.Goal;
import net.zerobuilder.compiler.analyse.Analyser;
import net.zerobuilder.compiler.analyse.ValidationException;
import net.zerobuilder.compiler.generate.DtoGeneratorInput.GeneratorInput;
import net.zerobuilder.compiler.generate.DtoGeneratorOutput.GeneratorOutput;
import net.zerobuilder.compiler.generate.Generator;

import javax.annotation.processing.AbstractProcessor;
import javax.annotation.processing.RoundEnvironment;
import javax.lang.model.SourceVersion;
import javax.lang.model.element.Element;
import javax.lang.model.element.ExecutableElement;
import javax.lang.model.element.TypeElement;
import javax.lang.model.util.Elements;
import javax.tools.JavaFileObject;
import java.io.IOException;
import java.io.Writer;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Stream;

import static java.util.stream.Collectors.toList;
import static javax.lang.model.util.ElementFilter.constructorsIn;
import static javax.lang.model.util.ElementFilter.methodsIn;
import static javax.lang.model.util.ElementFilter.typesIn;
import static javax.tools.Diagnostic.Kind.ERROR;
import static net.zerobuilder.compiler.Messages.ErrorMessages.GOAL_NOT_IN_BUILD;
import static net.zerobuilder.compiler.Messages.ErrorMessages.GOAL_WITHOUT_BUILDERS;
import static net.zerobuilder.compiler.Messages.JavadocMessages.generatedAnnotations;

public final class ZeroProcessor extends AbstractProcessor {

  @Override
  public Set<String> getSupportedAnnotationTypes() {
    return new HashSet<>(Arrays.asList(
        Goal.class.getName(),
        Builders.class.getName()));
  }

  @Override
  public SourceVersion getSupportedSourceVersion() {
    return SourceVersion.latestSupported();
  }

  @Override
  public boolean process(Set<? extends TypeElement> annotations, RoundEnvironment env) {
    Optional<? extends Element> goalNotInBuild = goalNotInBuild(env);
    if (goalNotInBuild.isPresent()) {
      return false;
    }
    Elements elements = processingEnv.getElementUtils();
    List<AnnotationSpec> generatedAnnotations = generatedAnnotations(elements);
    Set<TypeElement> types = typesIn(env.getElementsAnnotatedWith(Builders.class));
    for (TypeElement annotatedType : types) {
      try {
        GeneratorInput generatorInput = Analyser.analyse(annotatedType);
        GeneratorOutput generatorOutput = Generator.generate(generatorInput);
        TypeSpec typeSpec = generatorOutput.typeSpec(generatedAnnotations);
        try {
          write(generatorOutput.generatedType(), typeSpec);
        } catch (IOException e) {
          String message = "Error processing "
              + ClassName.get(annotatedType) + ": " + e.getMessage();
          processingEnv.getMessager().printMessage(ERROR, message, annotatedType);
          return false;
        }
      } catch (ValidationException e) {
        processingEnv.getMessager().printMessage(e.kind, e.getMessage(), e.about);
      } catch (RuntimeException e) {
        String message = "Error processing "
            + ClassName.get(annotatedType) + ": " + e.getMessage();
        processingEnv.getMessager().printMessage(ERROR, message, annotatedType);
        return false;
      }
    }
    return false;
  }

  private Optional<? extends Element> goalNotInBuild(RoundEnvironment env) {
    Set<? extends Element> elements = env.getElementsAnnotatedWith(Goal.class);
    Stream<ExecutableElement> methods = methodsIn(elements).stream();
    Stream<ExecutableElement> constructors = constructorsIn(elements).stream();
    for (ExecutableElement executableElement :
        Stream.concat(constructors, methods).collect(toList())) {
      if (executableElement.getEnclosingElement().getAnnotation(Builders.class) == null) {
        processingEnv.getMessager().printMessage(ERROR,
            GOAL_NOT_IN_BUILD, executableElement);
        return Optional.of(executableElement);
      }
    }
    for (TypeElement typeElement : typesIn(elements)) {
      if (typeElement.getAnnotation(Builders.class) == null) {
        processingEnv.getMessager().printMessage(ERROR,
            GOAL_WITHOUT_BUILDERS, typeElement);
        return Optional.of(typeElement);
      }
    }
    return Optional.empty();
  }

  private void write(ClassName generatedType, TypeSpec typeSpec) throws IOException {
    JavaFile javaFile = JavaFile.builder(generatedType.packageName(), typeSpec)
        .skipJavaLangImports(true)
        .build();
    JavaFileObject sourceFile = processingEnv.getFiler()
        .createSourceFile(generatedType.toString(),
            javaFile.typeSpec.originatingElements.toArray(new Element[0]));
    try (Writer writer = sourceFile.openWriter()) {
      writer.write(javaFile.toString());
    }
  }
}