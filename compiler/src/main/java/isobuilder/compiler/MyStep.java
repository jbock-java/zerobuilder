package isobuilder.compiler;

import com.google.common.base.Predicate;
import com.google.common.collect.ImmutableList;
import com.kaputtjars.isobuilder.Build;

import javax.annotation.processing.Messager;
import javax.lang.model.element.ExecutableElement;
import javax.lang.model.element.TypeElement;
import javax.lang.model.util.Elements;
import java.util.List;

import static com.google.common.collect.Iterables.all;
import static com.google.common.collect.Iterables.getOnlyElement;
import static isobuilder.compiler.Target.target;
import static javax.lang.model.util.ElementFilter.methodsIn;

final class MyStep {

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

  public void process(TypeElement typeElement) {
    ImmutableList.Builder<ValidationReport> reports = ImmutableList.builder();
    ImmutableList<ExecutableElement> targetMethods = getTargetMethods(typeElement);
    ValidationReport report = typeValidator.validateElement(typeElement, targetMethods);
    report.printMessagesTo(messager);
    reports.add(report);
    ExecutableElement targetMethod = getOnlyElement(targetMethods);
    report = methodValidator.validateElement(typeElement, targetMethod);
    report.printMessagesTo(messager);
    reports.add(report);
    report = matchValidator.validateElement(typeElement, targetMethod);
    report.printMessagesTo(messager);
    reports.add(report);
    if (!allClean(reports.build())) {
      // abort processing of this type
      return;
    }
    Target target = target(typeElement, targetMethod);
    myGenerator.generate(target);
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
      if (method.getAnnotation(Build.Via.class) != null) {
        builder.add(method);
      }
    }
    return builder.build();
  }

}
