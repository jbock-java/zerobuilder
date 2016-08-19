package isobuilder.compiler;

import com.google.auto.common.BasicAnnotationProcessor;
import com.google.auto.common.MoreElements;
import com.google.auto.common.MoreTypes;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.SetMultimap;
import com.squareup.javapoet.ClassName;
import isobuilder.Builder;

import javax.lang.model.element.Element;
import javax.lang.model.element.ExecutableElement;
import javax.lang.model.element.TypeElement;
import javax.lang.model.type.DeclaredType;
import javax.lang.model.type.TypeMirror;
import java.lang.annotation.Annotation;
import java.util.Set;

import static com.google.auto.common.MoreElements.asType;
import static com.google.auto.common.MoreTypes.asDeclared;
import static com.google.common.base.Preconditions.checkState;
import static com.google.common.collect.Iterables.getFirst;
import static isobuilder.compiler.BuilderInfo.factoryInfo;
import static javax.lang.model.element.Modifier.PRIVATE;
import static javax.lang.model.element.Modifier.STATIC;
import static javax.lang.model.util.ElementFilter.methodsIn;

public class BuilderStep implements BasicAnnotationProcessor.ProcessingStep {

  private final BuilderGenerator factoryGenerator;

  BuilderStep(BuilderGenerator factoryGenerator) {
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
    checkState(methods.size() == 1, "currently only one method per class allowed");
    ExecutableElement method = getFirst(methods, null);
      try {
        checkState(method.getModifiers().contains(STATIC), "method must be static");
        checkState(!method.getModifiers().contains(PRIVATE), "method cannot be private");
        DeclaredType returnType = asDeclared(method.getReturnType());
        TypeElement typeElement = asType(returnType.asElement());
        factoryGenerator.generate(factoryInfo(typeElement, method.getParameters()));
      } catch (SourceFileGenerationException e) {
        e.printStackTrace();
      }
    return ImmutableSet.of();
  }

}
