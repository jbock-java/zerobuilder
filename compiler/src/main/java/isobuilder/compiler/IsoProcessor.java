package isobuilder.compiler;

import com.google.auto.common.BasicAnnotationProcessor;
import com.google.auto.service.AutoService;
import com.google.common.collect.ImmutableList;

import javax.annotation.processing.Filer;
import javax.annotation.processing.Messager;
import javax.annotation.processing.Processor;
import javax.lang.model.SourceVersion;
import javax.lang.model.util.Elements;

@AutoService(Processor.class)
public class IsoProcessor extends BasicAnnotationProcessor {

  @Override
  public SourceVersion getSupportedSourceVersion() {
    return SourceVersion.latestSupported();
  }

  @Override
  protected Iterable<? extends ProcessingStep> initSteps() {
    Filer filer = processingEnv.getFiler();
    Elements elements = processingEnv.getElementUtils();
    BuilderGenerator factoryGenerator = new BuilderGenerator(filer, elements);
    Messager messager = processingEnv.getMessager();
    return ImmutableList.of(new BuilderStep(factoryGenerator, messager));
  }

}
