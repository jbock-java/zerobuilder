package net.zerobuilder.compiler;

import com.squareup.javapoet.AnnotationSpec;
import com.squareup.javapoet.ClassName;
import com.squareup.javapoet.JavaFile;
import com.squareup.javapoet.TypeSpec;
import net.zerobuilder.compiler.analyse.Analyser;
import net.zerobuilder.compiler.analyse.ValidationException;
import net.zerobuilder.compiler.common.LessTypes;
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
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Stream;

import static java.util.Collections.singletonList;
import static java.util.stream.Collectors.toList;
import static javax.lang.model.util.ElementFilter.constructorsIn;
import static javax.lang.model.util.ElementFilter.methodsIn;
import static javax.lang.model.util.ElementFilter.typesIn;
import static javax.tools.Diagnostic.Kind.ERROR;
import static net.zerobuilder.compiler.Messages.JavadocMessages.generatedAnnotations;
import static net.zerobuilder.compiler.generate.ZeroUtil.concat;

public final class ZeroProcessor extends AbstractProcessor {

  private final Set<TypeElement> done = new HashSet<>();

  @Override
  public Set<String> getSupportedAnnotationTypes() {
    return new HashSet<>(singletonList(
        Goal.class.getName()));
  }

  @Override
  public SourceVersion getSupportedSourceVersion() {
    return SourceVersion.latestSupported();
  }

  @Override
  public boolean process(Set<? extends TypeElement> annotations, RoundEnvironment env) {
    Elements elements = processingEnv.getElementUtils();
    List<AnnotationSpec> generatedAnnotations = generatedAnnotations(elements);
    Set<? extends Element> goals = env.getElementsAnnotatedWith(Goal.class);
    List<TypeElement> types =
        Stream.concat(methodsIn(goals).stream(),
            constructorsIn(goals).stream())
            .map(ExecutableElement::getEnclosingElement)
            .map(Element::asType)
            .map(LessTypes::asTypeElement)
            .collect(toList());
    types = concat(types, new ArrayList<>(typesIn(goals)));
    for (TypeElement enclosingElement : types) {
      try {
        if (!done.add(enclosingElement)) {
          continue;
        }
        GeneratorInput generatorInput = Analyser.analyse(enclosingElement);
        GeneratorOutput generatorOutput = Generator.generate(generatorInput);
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
        e.printStackTrace();
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
            javaFile.typeSpec.originatingElements.toArray(new Element[0]));
    try (Writer writer = sourceFile.openWriter()) {
      writer.write(javaFile.toString());
    }
  }
}