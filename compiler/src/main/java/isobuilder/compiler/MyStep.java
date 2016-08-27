package isobuilder.compiler;

import com.google.auto.common.BasicAnnotationProcessor;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.SetMultimap;
import com.kaputtjars.isobuilder.Build;

import javax.annotation.processing.Messager;
import javax.lang.model.element.Element;
import javax.lang.model.element.ExecutableElement;
import javax.lang.model.element.TypeElement;
import javax.lang.model.util.ElementFilter;
import java.lang.annotation.Annotation;
import java.util.List;
import java.util.Set;

import static isobuilder.compiler.Target.target;
import static javax.lang.model.util.ElementFilter.methodsIn;

final class MyStep implements BasicAnnotationProcessor.ProcessingStep {

  private final MyGenerator myGenerator;
  private final Messager messager;
  private final MethodValidator methodValidator = new MethodValidator();
  private final TypeValidator typeValidator = new TypeValidator();

  MyStep(MyGenerator myGenerator, Messager messager) {
    this.myGenerator = myGenerator;
    this.messager = messager;
  }

  @Override
  public Set<? extends Class<? extends Annotation>> annotations() {
    return ImmutableSet.of(Build.class);
  }

  @Override
  public Set<Element> process(SetMultimap<Class<? extends Annotation>, Element> elementsByAnnotation) {
    Set<TypeElement> types = ElementFilter.typesIn(elementsByAnnotation.get(Build.class));
    for (TypeElement typeElement : types) {
      try {
        ImmutableList<ExecutableElement> targetMethods = getTargetMethods(typeElement);
        ValidationReport typeReport = typeValidator.validateElement(typeElement, targetMethods);
        typeReport.printMessagesTo(messager);
        if (!typeReport.isClean()) {
          continue;
        }
        ExecutableElement targetMethod = targetMethods.get(0);
        ValidationReport methodReport = methodValidator.validateElement(typeElement, targetMethod);
        methodReport.printMessagesTo(messager);
        if (!methodReport.isClean()) {
          continue;
        }
        try {
          Target target = target(typeElement, targetMethod);
          myGenerator.generate(target);
        } catch (SourceFileGenerationException e) {
          e.printMessageTo(messager);
        }
      } catch (TypeNotPresentException e) {
        e.printStackTrace();
      }
    }
    return ImmutableSet.of();
  }

  static ImmutableList<ExecutableElement> getTargetMethods(TypeElement typeElement) {
    List<ExecutableElement> methods = methodsIn(typeElement.getEnclosedElements());
    ImmutableList.Builder<ExecutableElement> builder = ImmutableList.builder();
    for (ExecutableElement method : methods) {
      if (method.getAnnotation(Build.From.class) != null) {
        builder.add(method);
      }
    }
    return builder.build();
  }

}
