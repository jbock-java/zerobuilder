package net.zerobuilder.compiler;

import com.google.auto.service.AutoService;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableSet;
import net.zerobuilder.Build;

import javax.annotation.processing.AbstractProcessor;
import javax.annotation.processing.Filer;
import javax.annotation.processing.Messager;
import javax.annotation.processing.ProcessingEnvironment;
import javax.annotation.processing.Processor;
import javax.annotation.processing.RoundEnvironment;
import javax.lang.model.SourceVersion;
import javax.lang.model.element.Element;
import javax.lang.model.element.TypeElement;
import javax.lang.model.util.ElementFilter;
import javax.lang.model.util.Elements;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import static com.google.common.base.Throwables.getStackTraceAsString;
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

  private Logger logger;

  @Override
  public synchronized void init(ProcessingEnvironment processingEnv) {
    super.init(processingEnv);
    logger = new Logger(processingEnv.getMessager());
  }

  @Override
  public boolean process(Set<? extends TypeElement> annotations, RoundEnvironment roundEnv) {
    Filer filer = processingEnv.getFiler();
    Elements elements = processingEnv.getElementUtils();
    Messager messager = processingEnv.getMessager();
    MyGenerator generator = new MyGenerator(filer, elements, messager);
    MyStep myStep = new MyStep(generator, messager, elements);

    List<TypeElement> deferredTypes = new ArrayList<TypeElement>();
    if (roundEnv.processingOver()) {
      // This means that the previous round didn't generate any new sources, so we can't have found
      // any new instances of @AutoValue; and we can't have any new types that are the reason a type
      // was in deferredTypes.
      for (TypeElement type : deferredTypes) {
        logger.error("Did not generate class for " + type.getQualifiedName()
            + " because it references undefined types", type);
      }
      return false;
    }
    ImmutableSet<? extends Element> annotatedElements =
        ImmutableSet.copyOf(roundEnv.getElementsAnnotatedWith(Build.class));
    List<TypeElement> types = new ImmutableList.Builder<TypeElement>()
        .addAll(deferredTypes)
        .addAll(ElementFilter.typesIn(annotatedElements))
        .build();
    for (TypeElement type : types) {
      try {
        myStep.process(type);
      } catch (RuntimeException e) {
        String trace = getStackTraceAsString(e);
        logger.error("@AutoValue processor threw an exception: " + trace, type);
      }
    }
    return false;
  }

  private static class Logger {

    private final Messager messager;

    Logger(Messager messager) {
      this.messager = messager;
    }

    void error(String msg, Element e) {
      messager.printMessage(ERROR, msg, e);
    }
  }

}
