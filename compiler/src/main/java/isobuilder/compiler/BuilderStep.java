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

import static isobuilder.compiler.Target.target;
import static javax.lang.model.util.ElementFilter.methodsIn;

public class BuilderStep implements BasicAnnotationProcessor.ProcessingStep {

  private final ContractGenerator contractGenerator;
  private final Messager messager;
  private final MethodValidator methodValidator = new MethodValidator();
  private final DuplicateValidator duplicateValidator = new DuplicateValidator();

  @Inject
  BuilderStep(ContractGenerator contractGenerator, Messager messager) {
    this.contractGenerator = contractGenerator;
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
    for (ExecutableElement method : methods) {
      Target target = target(method);
      ValidationReport<ExecutableElement> methodReport = methodValidator.validateMethod(method);
      ValidationReport<ExecutableElement> duplicateReport = duplicateValidator.validateClassname(target);
      methodReport.printMessagesTo(messager);
      duplicateReport.printMessagesTo(messager);
      if (methodReport.isClean() && duplicateReport.isClean()) {
        try {
          contractGenerator.generate(target);
        } catch (SourceFileGenerationException e) {
          e.printMessageTo(messager);
        }
      }
    }
    return ImmutableSet.of();
  }

}
