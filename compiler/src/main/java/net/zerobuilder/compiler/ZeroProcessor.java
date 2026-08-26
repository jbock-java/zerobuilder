package net.zerobuilder.compiler;

import com.palantir.javapoet.ClassName;
import com.palantir.javapoet.JavaFile;
import com.palantir.javapoet.TypeSpec;
import java.io.IOException;
import java.io.Writer;
import java.util.HashSet;
import java.util.Set;
import java.util.stream.Stream;
import javax.annotation.processing.AbstractProcessor;
import javax.annotation.processing.RoundEnvironment;
import javax.lang.model.SourceVersion;
import javax.lang.model.element.Element;
import javax.lang.model.element.TypeElement;
import javax.tools.JavaFileObject;
import net.zerobuilder.RecordBuilder;
import net.zerobuilder.compiler.analyse.Analyser;
import net.zerobuilder.compiler.analyse.ValidationException;
import net.zerobuilder.compiler.generate.DtoGeneratorOutput.GeneratorOutput;
import net.zerobuilder.compiler.generate.Generator;
import net.zerobuilder.compiler.generate.GoalDescription;

import static java.util.stream.Collectors.toSet;
import static javax.lang.model.util.ElementFilter.typesIn;
import static javax.tools.Diagnostic.Kind.ERROR;

public final class ZeroProcessor extends AbstractProcessor {

  private final Set<TypeElement> done = new HashSet<>();

  @Override
  public Set<String> getSupportedAnnotationTypes() {
    return Stream.of(
            RecordBuilder.class)
        .map(Class::getName)
        .collect(toSet());
  }

  @Override
  public SourceVersion getSupportedSourceVersion() {
    return SourceVersion.latestSupported();
  }

  @Override
  public boolean process(Set<? extends TypeElement> annotations, RoundEnvironment env) {
    for (TypeElement enclosingElement : typesIn(env.getElementsAnnotatedWith(RecordBuilder.class))) {
      try {
        if (!done.add(enclosingElement)) {
          continue;
        }
        GoalDescription description = Analyser.analyse(enclosingElement);
        GeneratorOutput generatorOutput = Generator.generate(description);
        TypeSpec typeSpec = generatorOutput.typeSpec();
        try {
          write(generatorOutput.generatedType(), typeSpec);
        } catch (IOException e) {
          String message = "Error processing "
              + ClassName.get(enclosingElement) + ": " + e.getMessage();
          processingEnv.getMessager().printMessage(ERROR, message, enclosingElement);
          return false;
        }
      } catch (ValidationException e) {
        processingEnv.getMessager().printMessage(e.kind, e.getMessage(), e.about);
      } catch (RuntimeException e) {
        e.printStackTrace(); // keep
        String message = "Error processing "
            + ClassName.get(enclosingElement) + ": " + e.getMessage();
        processingEnv.getMessager().printMessage(ERROR, message, enclosingElement);
        return false;
      }
    }
    return false;
  }

  private void write(ClassName generatedType, TypeSpec typeSpec) throws IOException {
    JavaFile javaFile = JavaFile.builder(generatedType.packageName(), typeSpec)
        .skipJavaLangImports(true)
        .build();
    JavaFileObject sourceFile = processingEnv.getFiler()
        .createSourceFile(generatedType.toString(),
            javaFile.typeSpec().originatingElements().toArray(new Element[0]));
    try (Writer writer = sourceFile.openWriter()) {
      writer.write(javaFile.toString());
    }
  }
}
