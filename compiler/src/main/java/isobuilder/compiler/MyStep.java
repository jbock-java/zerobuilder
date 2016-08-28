package isobuilder.compiler;

import com.google.auto.common.BasicAnnotationProcessor;
import com.google.common.base.Predicate;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Iterables;
import com.google.common.collect.SetMultimap;
import com.kaputtjars.isobuilder.Build;

import javax.annotation.processing.Messager;
import javax.lang.model.element.Element;
import javax.lang.model.element.ExecutableElement;
import javax.lang.model.element.TypeElement;
import javax.lang.model.util.ElementFilter;
import javax.lang.model.util.Elements;
import javax.tools.Diagnostic;
import java.lang.annotation.Annotation;
import java.util.List;
import java.util.Set;

import static com.google.common.collect.Iterables.all;
import static com.google.common.collect.Iterables.getOnlyElement;
import static isobuilder.compiler.Target.target;
import static javax.lang.model.util.ElementFilter.methodsIn;
import static javax.tools.Diagnostic.Kind.WARNING;

final class MyStep implements BasicAnnotationProcessor.ProcessingStep {

  private final MyGenerator myGenerator;
  private final Messager messager;
  private final MethodValidator methodValidator = new MethodValidator();
  private final TypeValidator typeValidator = new TypeValidator();
  private final MatchValidator matchValidator;

  MyStep(MyGenerator myGenerator, Messager messager, Elements elements) {
    this.myGenerator = myGenerator;
    this.messager = messager;
    this.matchValidator = new MatchValidator(elements);
  }

  @Override
  public Set<? extends Class<? extends Annotation>> annotations() {
    return ImmutableSet.of(Build.class);
  }

  @Override
  public Set<Element> process(SetMultimap<Class<? extends Annotation>, Element> elementsByAnnotation) {
    Set<TypeElement> types = ElementFilter.typesIn(elementsByAnnotation.get(Build.class));
    ImmutableList.Builder<ValidationReport> reports = ImmutableList.builder();
    for (TypeElement typeElement : types) {
      ImmutableList<ExecutableElement> targetMethods = getTargetMethods(typeElement);
      ValidationReport report = typeValidator.validateElement(typeElement, targetMethods);
      report.printMessagesTo(messager);
      reports.add(report);
      ExecutableElement targetMethod = targetMethods.get(0);
      report = methodValidator.validateElement(typeElement, targetMethod);
      report.printMessagesTo(messager);
      reports.add(report);
      report = matchValidator.validateElement(typeElement, targetMethod);
      report.printMessagesTo(messager);
      reports.add(report);
    }
    if (!allClean(reports.build())) {
      messager.printMessage(WARNING, "Processing aborted with errors.");
      return ImmutableSet.of();
    }
    for (TypeElement typeElement : types) {
      try {
        ExecutableElement targetMethod = getOnlyElement(getTargetMethods(typeElement));
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

  private boolean allClean(ImmutableList<ValidationReport> reports) {
    return all(reports, new Predicate<ValidationReport>() {
      @Override
      public boolean apply(ValidationReport report) {
        return report.isClean();
      }
    });
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
