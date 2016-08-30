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

import static com.google.common.collect.Iterables.all;
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

  public void process(TypeElement buildElement) {
    ValidationReport<TypeElement, ExecutableElement> typeReport = typeValidator
        .validateElement(buildElement, getTargetMethods(buildElement)).printMessagesTo(messager);
    if (!typeReport.isClean()) {
      return;
    }
    ValidationReport<TypeElement, ?> methodReport = methodValidator
        .validateElement(buildElement, typeReport.payload.get()).printMessagesTo(messager);
    ValidationReport<TypeElement, AccessType> matchReport = MatchValidator.builder()
        .elements(elements).buildViaElement(typeReport.payload.get())
        .buildElement(buildElement).build().validate().printMessagesTo(messager);
    if (!allClean(methodReport, matchReport)) {
      // abort processing of this type
      return;
    }
    MyContext context = MyContext.target(buildElement, typeReport.payload.get(), matchReport.payload.get());
    myGenerator.generate(context);
  }

  private boolean allClean(ValidationReport... reports) {
    return all(ImmutableList.copyOf(reports), new Predicate<ValidationReport>() {
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
