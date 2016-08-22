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
import static isobuilder.compiler.Target.target;
import static javax.lang.model.util.ElementFilter.methodsIn;

public class ContractStep implements BasicAnnotationProcessor.ProcessingStep {

  private final ContractGenerator generator;
  private final Messager messager;

  @Inject
  ContractStep(ContractGenerator generator, Messager messager) {
    this.generator = generator;
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
      generator.generate(target(method));
    } catch (SourceFileGenerationException e) {
      e.printMessageTo(messager);
    }
    return ImmutableSet.of();
  }

}
