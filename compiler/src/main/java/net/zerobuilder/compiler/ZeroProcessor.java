package net.zerobuilder.compiler;

import com.palantir.javapoet.AnnotationSpec;
import com.palantir.javapoet.ClassName;
import com.palantir.javapoet.JavaFile;
import com.palantir.javapoet.TypeSpec;
import java.io.IOException;
import java.io.Writer;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Stream;
import javax.annotation.processing.AbstractProcessor;
import javax.annotation.processing.RoundEnvironment;
import javax.lang.model.SourceVersion;
import javax.lang.model.element.Element;
import javax.lang.model.element.ExecutableElement;
import javax.lang.model.element.TypeElement;
import javax.tools.JavaFileObject;
import net.zerobuilder.Builder;
import net.zerobuilder.RecordBuilder;
import net.zerobuilder.compiler.analyse.Analyser;
import net.zerobuilder.compiler.analyse.ValidationException;
import net.zerobuilder.compiler.common.LessTypes;
import net.zerobuilder.compiler.generate.DtoGeneratorOutput.GeneratorOutput;
import net.zerobuilder.compiler.generate.GoalDescription;
import net.zerobuilder.compiler.generate.Generator;

import static java.util.stream.Collectors.toSet;
import static javax.lang.model.util.ElementFilter.constructorsIn;
import static javax.lang.model.util.ElementFilter.typesIn;
import static javax.tools.Diagnostic.Kind.ERROR;
import static net.zerobuilder.compiler.Messages.JavadocMessages.generatedAnnotations;

public final class ZeroProcessor extends AbstractProcessor {

  private final Set<TypeElement> done = new HashSet<>();

  @Override
  public Set<String> getSupportedAnnotationTypes() {
    return Stream.of(
            Builder.class,
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
    List<AnnotationSpec> generatedAnnotations = generatedAnnotations();
    Set<TypeElement> types = new LinkedHashSet<>();
    constructorsIn(env.getElementsAnnotatedWith(Builder.class)).stream()
        .map(ExecutableElement::getEnclosingElement)
        .map(Element::asType)
        .map(LessTypes::asTypeElement)
        .forEach(types::add);
    types.addAll(typesIn(env.getElementsAnnotatedWith(RecordBuilder.class)));
    for (TypeElement enclosingElement : types) {
      try {
        if (!done.add(enclosingElement)) {
          continue;
        }
        GoalDescription description = Analyser.analyse(enclosingElement);
        GeneratorOutput generatorOutput = Generator.generate(description);
        TypeSpec typeSpec = generatorOutput.typeSpec(generatedAnnotations);
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
