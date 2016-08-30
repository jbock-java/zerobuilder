package net.zerobuilder.compiler;

import com.google.auto.common.MoreElements;
import com.google.common.base.Optional;
import com.google.common.base.Predicate;
import com.google.common.collect.ImmutableList;
import com.squareup.javapoet.ClassName;
import net.zerobuilder.Build;
import net.zerobuilder.compiler.MyContext.AccessType;

import javax.annotation.processing.Messager;
import javax.lang.model.element.AnnotationMirror;
import javax.lang.model.element.AnnotationValue;
import javax.lang.model.element.ExecutableElement;
import javax.lang.model.element.TypeElement;
import javax.lang.model.type.TypeMirror;
import javax.lang.model.util.Elements;
import javax.lang.model.util.SimpleAnnotationValueVisitor7;
import java.util.Map;

import static com.google.auto.common.MoreTypes.asTypeElement;
import static com.google.common.base.Optional.absent;
import static com.google.common.collect.Iterables.all;
import static net.zerobuilder.compiler.MatchValidator.skipMatchValidation;
import static net.zerobuilder.compiler.MyContext.createContext;

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
    ClassName buildGoal = goal(buildElement);
    boolean toBuilder = buildElement.getAnnotation(Build.class).toBuilder();
    ValidationReport<TypeElement, ExecutableElement> typeReport = typeValidator
        .validateElement(buildElement, buildGoal);
    if (!typeReport.isClean(messager)) {
      return;
    }
    ValidationReport<TypeElement, ?> methodReport = methodValidator
        .validateVia(buildGoal, typeReport.payload.get());
    ValidationReport<TypeElement, AccessType> matchReport = toBuilder
        ? MatchValidator.builder().elements(elements).buildViaElement(typeReport.payload.get()).buildElement(buildElement).build().validate()
        : skipMatchValidation(buildElement);
    if (!allClean(methodReport, matchReport)) {
      // abort processing of this type
      return;
    }
    MyContext context = createContext(buildGoal, buildElement, typeReport.payload.get(),
        matchReport.payload.get());
    myGenerator.generate(context);
  }

  private boolean allClean(ValidationReport... reports) {
    return all(ImmutableList.copyOf(reports), new Predicate<ValidationReport>() {
      @Override
      public boolean apply(ValidationReport report) {
        return report.isClean(messager);
      }
    });
  }

  private static ClassName goal(TypeElement buildElement) {
    Optional<AnnotationValue> annotationValue = getAnnotationValue(buildElement, "goal");
    if (!annotationValue.isPresent()) {
      return ClassName.get(buildElement);
    }
    TypeMirror accept = annotationValue.get().accept(new SimpleAnnotationValueVisitor7<TypeMirror, Void>() {
      @Override
      public TypeMirror visitType(TypeMirror typeMirror, Void aVoid) {
        return typeMirror;
      }
    }, null);
    if (accept == null) {
      return ClassName.get(buildElement);
    }
    ClassName className = ClassName.get(asTypeElement(accept));
    if (className.equals(ClassName.get(Void.class))) {
      return ClassName.get(buildElement);
    }
    return className;
  }

  private static Optional<AnnotationValue> getAnnotationValue(TypeElement buildElement, String attributeName) {
    Optional<AnnotationMirror> annotationMirror = MoreElements.getAnnotationMirror(buildElement, Build.class);
    if (!annotationMirror.isPresent()) {
      return absent();
    }
    AnnotationMirror am = annotationMirror.get();
    for (Map.Entry<? extends ExecutableElement, ? extends AnnotationValue> entry : am.getElementValues().entrySet()) {
      if (attributeName.equals(entry.getKey().getSimpleName().toString())) {
        return Optional.of(entry.getValue());
      }
    }
    return absent();
  }

}
