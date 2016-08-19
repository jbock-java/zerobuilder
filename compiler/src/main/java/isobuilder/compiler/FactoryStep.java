package isobuilder.compiler;

import com.google.auto.common.BasicAnnotationProcessor;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.SetMultimap;
import isobuilder.Builder;

import javax.lang.model.element.Element;
import javax.lang.model.element.ExecutableElement;
import javax.lang.model.element.Modifier;
import javax.lang.model.element.TypeElement;
import java.lang.annotation.Annotation;
import java.util.Set;

import static com.google.auto.common.MoreElements.asType;
import static com.google.common.base.Preconditions.checkState;
import static isobuilder.compiler.FactoryInfo.factoryInfo;
import static javax.lang.model.util.ElementFilter.methodsIn;

public class FactoryStep implements BasicAnnotationProcessor.ProcessingStep {

  private final FactoryGenerator factoryGenerator;

  FactoryStep(FactoryGenerator factoryGenerator) {
    this.factoryGenerator = factoryGenerator;
  }

  @Override
  public Set<? extends Class<? extends Annotation>> annotations() {
    return ImmutableSet.of(Builder.class);
  }

  @Override
  public Set<Element> process(SetMultimap<Class<? extends Annotation>, Element> elementsByAnnotation) {
    Set<Element> elements = elementsByAnnotation.get(Builder.class);
    Set<ExecutableElement> methods = methodsIn(elements);
    checkState(methods.size() == 1, "currently only one method allowed");
    for (ExecutableElement element : methods) {
      try {
        checkState(element.getModifiers().contains(Modifier.STATIC), "target must be static");
        checkState(!element.getModifiers().contains(Modifier.PRIVATE), "target cannot be private");
        TypeElement typeElement = asType(element.getEnclosingElement());
        factoryGenerator.generate(factoryInfo(typeElement, element.getParameters()));
      } catch (SourceFileGenerationException e) {
        e.printStackTrace();
      }
    }
    return ImmutableSet.of();
  }

}
