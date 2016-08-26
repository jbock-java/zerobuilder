package isobuilder.compiler;

import com.google.auto.common.BasicAnnotationProcessor;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.SetMultimap;
import com.google.common.collect.Sets;
import isobuilder.Builder;

import javax.annotation.processing.Messager;
import javax.lang.model.element.Element;
import javax.lang.model.element.ExecutableElement;
import java.lang.annotation.Annotation;
import java.util.Set;

import static isobuilder.compiler.Target.target;
import static javax.lang.model.util.ElementFilter.constructorsIn;
import static javax.lang.model.util.ElementFilter.methodsIn;

final class MyStep implements BasicAnnotationProcessor.ProcessingStep {

  private final MyGenerator myGenerator;
  private final Messager messager;
  private final MethodValidator methodValidator = new MethodValidator();
  private final DuplicateValidator duplicateValidator = new DuplicateValidator();

  MyStep(MyGenerator myGenerator, Messager messager) {
    this.myGenerator = myGenerator;
    this.messager = messager;
  }

  @Override
  public Set<? extends Class<? extends Annotation>> annotations() {
    return ImmutableSet.of(Builder.class);
  }

  @Override
  public Set<Element> process(SetMultimap<Class<? extends Annotation>, Element> elementsByAnnotation) {
    Set<Element> elements = elementsByAnnotation.get(Builder.class);
    Set<ExecutableElement> methods = Sets.union(methodsIn(elements), constructorsIn(elements));
    for (ExecutableElement method : methods) {
      try {
        ValidationReport methodReport = methodValidator.validateElement(method);
        ValidationReport duplicateReport = duplicateValidator.validateClassname(method);
        methodReport.printMessagesTo(messager);
        duplicateReport.printMessagesTo(messager);
        if (methodReport.isClean() && duplicateReport.isClean()) {
          try {
            Target target = target(method);
            myGenerator.generate(target);
          } catch (SourceFileGenerationException e) {
            e.printMessageTo(messager);
          }
        }
      } catch (TypeNotPresentException e) {
        e.printStackTrace();
      }
    }
    return ImmutableSet.of();
  }

}
