package net.zerobuilder.compiler;

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

import static com.google.auto.common.MoreElements.getAnnotationMirror;
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
  private final MatchValidator.BuilderFactory matchValidatorFactory;

  MyStep(MyGenerator myGenerator, Messager messager, Elements elements) {
    this.myGenerator = myGenerator;
    this.messager = messager;
    this.matchValidatorFactory = new MatchValidator.BuilderFactory(elements);
  }

  void process(TypeElement buildElement) {
    ClassName goalType = goalTypeFromAnnotation(buildElement).or(ClassName.get(buildElement));
    boolean toBuilder = buildElement.getAnnotation(Build.class).toBuilder();
    ValidationReport<TypeElement, ExecutableElement> typeReport = typeValidator
        .validateElement(buildElement, goalType);
    if (!typeReport.isClean(messager)) {
      return;
    }
    ExecutableElement buildVia = typeReport.payload.get();
    ValidationReport<TypeElement, ?> methodReport = methodValidator
        .validateVia(goalType, buildVia);
    ValidationReport<TypeElement, AccessType> matchReport = toBuilder
        ? matchValidatorFactory.buildViaElement(buildVia).buildElement(buildElement).validate()
        : skipMatchValidation(buildElement);
    if (!allClean(methodReport, matchReport)) {
      // abort processing this type
      return;
    }
    MyContext context = createContext(goalType, buildElement, buildVia,
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

  private static Optional<ClassName> goalTypeFromAnnotation(TypeElement buildElement) {
    Optional<AnnotationValue> annotationValue = annotationValue(buildElement, "goal");
    if (!annotationValue.isPresent()) {
      return absent();
    }
    TypeMirror accept = annotationValue.get().accept(new SimpleAnnotationValueVisitor7<TypeMirror, Void>() {
      @Override
      public TypeMirror visitType(TypeMirror typeMirror, Void aVoid) {
        return typeMirror;
      }
    }, null);
    if (accept == null) {
      return absent();
    }
    ClassName className = ClassName.get(asTypeElement(accept));
    if (className.equals(ClassName.get(Void.class))) {
      return absent();
    }
    return Optional.of(className);
  }

  private static Optional<AnnotationValue> annotationValue(TypeElement buildElement, String attributeName) {
    Optional<AnnotationMirror> annotationMirror = getAnnotationMirror(buildElement, Build.class);
    if (!annotationMirror.isPresent()) {
      return absent();
    }
    for (Map.Entry<? extends ExecutableElement, ? extends AnnotationValue> entry :
        annotationMirror.get().getElementValues().entrySet()) {
      if (attributeName.equals(entry.getKey().getSimpleName().toString())) {
        return Optional.of(entry.getValue());
      }
    }
    return absent();
  }

}
