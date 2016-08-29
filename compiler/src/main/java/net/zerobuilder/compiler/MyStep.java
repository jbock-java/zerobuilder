package net.zerobuilder.compiler;

import com.google.common.base.Predicate;
import com.google.common.collect.FluentIterable;
import com.google.common.collect.ImmutableList;
import net.zerobuilder.Build;
import net.zerobuilder.compiler.MyContext.AccessType;

import javax.annotation.processing.Messager;
import javax.lang.model.element.ExecutableElement;
import javax.lang.model.element.TypeElement;
import javax.lang.model.util.Elements;
import java.util.List;

import static com.google.common.collect.Iterables.all;
import static com.google.common.collect.Iterables.getOnlyElement;
import static javax.lang.model.util.ElementFilter.constructorsIn;
import static javax.lang.model.util.ElementFilter.methodsIn;

final class MyStep {

  private final MyGenerator myGenerator;
  private final Messager messager;
  private final MethodValidator methodValidator = new MethodValidator();
  private final TypeValidator typeValidator = new TypeValidator();
  private final Elements elements;

  MyStep(MyGenerator myGenerator, Messager messager, Elements elements) {
    this.myGenerator = myGenerator;
    this.messager = messager;
    this.elements = elements;
  }

  public void process(TypeElement typeElement) {
    ImmutableList.Builder<ValidationReport> reports = ImmutableList.builder();
    ImmutableList<ExecutableElement> targetMethods = getTargetMethods(typeElement);
    ValidationReport<TypeElement, ?> report = typeValidator.validateElement(typeElement, targetMethods);
    report.printMessagesTo(messager);
    reports.add(report);
    ExecutableElement targetMethod = getOnlyElement(targetMethods);
    ValidationReport<TypeElement, ?> methodReport = methodValidator.validateElement(typeElement, targetMethod);
    methodReport.printMessagesTo(messager);
    reports.add(methodReport);
    MatchValidator matchValidator = MatchValidator.create(typeElement, targetMethod, elements);
    ValidationReport<TypeElement, AccessType> matchReport = matchValidator.validate();
    matchReport.printMessagesTo(messager);
    reports.add(matchReport);
    if (!allClean(reports.build())) {
      // abort processing of this type
      return;
    }
    MyContext context = MyContext.target(typeElement, targetMethod, matchReport.payload.get());
    myGenerator.generate(context);
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
    return FluentIterable.from(methodsIn(typeElement.getEnclosedElements()))
        .append(constructorsIn(typeElement.getEnclosedElements()))
        .filter(new Predicate<ExecutableElement>() {
          @Override
          public boolean apply(ExecutableElement element) {
            return element.getAnnotation(Build.Via.class) != null;
          }
        }).toList();
  }

}
