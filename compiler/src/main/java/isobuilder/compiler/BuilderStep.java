package isobuilder.compiler;

import com.google.auto.common.BasicAnnotationProcessor;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.SetMultimap;
import isobuilder.Builder;

import javax.annotation.processing.Messager;
import javax.inject.Inject;
import javax.lang.model.element.Element;
import javax.lang.model.element.ExecutableElement;
import java.lang.annotation.Annotation;
import java.util.Set;

import static com.google.common.collect.Iterables.getFirst;
import static javax.lang.model.util.ElementFilter.methodsIn;

public class BuilderStep implements BasicAnnotationProcessor.ProcessingStep {

  private final BuilderGenerator factoryGenerator;
  private final Messager messager;

  @Inject
  BuilderStep(BuilderGenerator builderGenerator, Messager messager) {
    this.factoryGenerator = builderGenerator;
    this.messager = messager;
  }

  @Override
  public Set<? extends Class<? extends Annotation>> annotations() {
    return ImmutableSet.of(Builder.class);
  }

  @Override
  public Set<Element> process(SetMultimap<Class<? extends Annotation>, Element> elementsByAnnotation) {
    Set<Element> elements = elementsByAnnotation.get(Builder.class);
    Set<ExecutableElement> methods = methodsIn(elements);
    ExecutableElement method = getFirst(methods, null);
    try {
      factoryGenerator.generate(method);
    } catch (SourceFileGenerationException e) {
      e.printMessageTo(messager);
    }
    return ImmutableSet.of();
  }

}
