package net.zerobuilder.compiler;

import com.google.auto.service.AutoService;
import com.google.common.collect.ImmutableSet;
import net.zerobuilder.Build;

import javax.annotation.processing.AbstractProcessor;
import javax.annotation.processing.Filer;
import javax.annotation.processing.Messager;
import javax.annotation.processing.Processor;
import javax.annotation.processing.RoundEnvironment;
import javax.lang.model.SourceVersion;
import javax.lang.model.element.TypeElement;
import javax.lang.model.util.Elements;
import java.util.Set;

import static com.google.common.base.Throwables.getStackTraceAsString;
import static javax.lang.model.util.ElementFilter.typesIn;
import static javax.tools.Diagnostic.Kind.ERROR;

@AutoService(Processor.class)
public final class MyProcessor extends AbstractProcessor {

  @Override
  public Set<String> getSupportedAnnotationTypes() {
    return ImmutableSet.of(Build.class.getName());
  }

  @Override
  public SourceVersion getSupportedSourceVersion() {
    return SourceVersion.latestSupported();
  }

  @Override
  public boolean process(Set<? extends TypeElement> annotations, RoundEnvironment roundEnv) {
    Filer filer = processingEnv.getFiler();
    Elements elements = processingEnv.getElementUtils();
    Messager messager = processingEnv.getMessager();
    MyGenerator generator = new MyGenerator(filer, elements);
    MyStep myStep = new MyStep(generator, messager, elements);
    Set<TypeElement> types = typesIn(roundEnv.getElementsAnnotatedWith(Build.class));
    for (TypeElement type : types) {
      try {
        myStep.process(type);
      } catch (RuntimeException e) {
        messager.printMessage(ERROR, "Code generation failed: " + getStackTraceAsString(e), type);
      }
    }
    return false;
  }

}
