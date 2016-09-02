package net.zerobuilder.compiler;

import com.google.common.collect.ImmutableList;
import com.squareup.javapoet.ClassName;
import com.squareup.javapoet.TypeName;
import net.zerobuilder.Build;
import net.zerobuilder.compiler.MyContext.ProjectionType;

import javax.annotation.processing.Messager;
import javax.lang.model.element.ExecutableElement;
import javax.lang.model.element.TypeElement;
import javax.lang.model.util.Elements;

import static com.google.common.collect.Iterables.concat;
import static com.google.common.collect.Iterables.getOnlyElement;
import static javax.lang.model.element.ElementKind.CONSTRUCTOR;
import static javax.lang.model.element.Modifier.PRIVATE;
import static javax.lang.model.element.Modifier.STATIC;
import static javax.lang.model.util.ElementFilter.constructorsIn;
import static javax.lang.model.util.ElementFilter.methodsIn;
import static net.zerobuilder.compiler.Messages.ErrorMessages.COULD_NOT_GUESS_VIA;
import static net.zerobuilder.compiler.Messages.ErrorMessages.NOT_ENOUGH_PARAMETERS;
import static net.zerobuilder.compiler.Messages.ErrorMessages.PRIVATE_METHOD;
import static net.zerobuilder.compiler.Messages.ErrorMessages.SEVERAL_VIA_ANNOTATIONS;
import static net.zerobuilder.compiler.MyContext.createContext;

final class MyStep {

  private final MyGenerator myGenerator;
  private final Messager messager;
  private final TypeValidator typeValidator = new TypeValidator();
  private final MatchValidator.Factory matchValidator;

  MyStep(MyGenerator myGenerator, Messager messager, Elements elements) {
    this.myGenerator = myGenerator;
    this.messager = messager;
    this.matchValidator = new MatchValidator.Factory(elements);
  }

  void process(TypeElement buildElement) {
    try {
      ExecutableElement buildVia = viaAnnotatedElement(buildElement);
      TypeName goalType = buildVia.getKind() == CONSTRUCTOR ?
          ClassName.get(buildElement) :
          TypeName.get(buildVia.getReturnType());
      typeValidator.validateBuildType(buildElement);
      ProjectionType projectionType = buildElement.getAnnotation(Build.class).toBuilder()
          ? matchValidator.buildViaElement(buildVia).buildElement(buildElement).validate()
          : ProjectionType.NONE;
      MyContext context = createContext(goalType, buildElement, buildVia, projectionType,
          buildElement.getAnnotation(Build.class).nogc());
      myGenerator.generate(context);
    } catch (ValidationException e) {
      e.printMessage(messager);
    }
  }


  private ExecutableElement viaAnnotatedElement(TypeElement buildElement) throws ValidationException {
    ImmutableList.Builder<ExecutableElement> builder = ImmutableList.builder();
    for (ExecutableElement executableElement : concat(constructorsIn(buildElement.getEnclosedElements()),
        methodsIn(buildElement.getEnclosedElements()))) {
      if (executableElement.getAnnotation(Build.Goal.class) != null) {
        if (executableElement.getModifiers().contains(PRIVATE)) {
          throw new ValidationException(PRIVATE_METHOD, buildElement);
        }
        if (executableElement.getParameters().isEmpty()) {
          throw new ValidationException(NOT_ENOUGH_PARAMETERS, buildElement);
        }
        builder.add(executableElement);
      }
    }
    switch (builder.build().size()) {
      case 0:
        return guessVia(buildElement);
      case 1:
        return getOnlyElement(builder.build());
      default:
        throw new ValidationException(SEVERAL_VIA_ANNOTATIONS, buildElement);
    }
  }

  private ExecutableElement guessVia(TypeElement buildElement) throws ValidationException {
    ImmutableList.Builder<ExecutableElement> builder = ImmutableList.builder();
    for (ExecutableElement executableElement : constructorsIn(buildElement.getEnclosedElements())) {
      if (!executableElement.getModifiers().contains(PRIVATE)
          && !executableElement.getParameters().isEmpty()) {
        builder.add(executableElement);
      }
    }
    if (builder.build().size() == 1) {
      return getOnlyElement(builder.build());
    }
    builder = ImmutableList.builder();
    for (ExecutableElement executableElement : methodsIn(buildElement.getEnclosedElements())) {
      if (!executableElement.getModifiers().contains(PRIVATE)
          && executableElement.getModifiers().contains(STATIC)
          && !executableElement.getParameters().isEmpty()) {
        builder.add(executableElement);
      }
    }
    if (builder.build().size() == 1) {
      return getOnlyElement(builder.build());
    }
    builder = ImmutableList.builder();
    for (ExecutableElement executableElement : methodsIn(buildElement.getEnclosedElements())) {
      if (!executableElement.getModifiers().contains(PRIVATE)
          && !executableElement.getParameters().isEmpty()) {
        builder.add(executableElement);
      }
    }
    if (builder.build().size() == 1) {
      return getOnlyElement(builder.build());
    }
    throw new ValidationException(COULD_NOT_GUESS_VIA, buildElement);
  }

}
