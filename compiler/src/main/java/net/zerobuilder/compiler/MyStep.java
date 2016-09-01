package net.zerobuilder.compiler;

import com.google.common.collect.ImmutableList;
import com.squareup.javapoet.ClassName;
import com.squareup.javapoet.TypeName;
import net.zerobuilder.Build;
import net.zerobuilder.compiler.MyContext.AccessType;

import javax.annotation.processing.Messager;
import javax.lang.model.element.ExecutableElement;
import javax.lang.model.element.TypeElement;
import javax.lang.model.util.Elements;

import static com.google.common.collect.Iterables.concat;
import static com.google.common.collect.Iterables.getOnlyElement;
import static javax.lang.model.element.ElementKind.CONSTRUCTOR;
import static javax.lang.model.element.Modifier.PRIVATE;
import static javax.lang.model.util.ElementFilter.constructorsIn;
import static javax.lang.model.util.ElementFilter.methodsIn;
import static net.zerobuilder.compiler.Messages.ErrorMessages.COULD_NOT_GUESS_VIA;
import static net.zerobuilder.compiler.Messages.ErrorMessages.PRIVATE_METHOD;
import static net.zerobuilder.compiler.Messages.ErrorMessages.SEVERAL_VIA_ANNOTATIONS;
import static net.zerobuilder.compiler.MyContext.createContext;

final class MyStep {

  private final MyGenerator myGenerator;
  private final Messager messager;
  private final TypeValidator typeValidator = new TypeValidator();
  private final MatchValidator.BuilderFactory matchValidatorFactory;

  MyStep(MyGenerator myGenerator, Messager messager, Elements elements) {
    this.myGenerator = myGenerator;
    this.messager = messager;
    this.matchValidatorFactory = new MatchValidator.BuilderFactory(elements);
  }

  void process(TypeElement buildElement) {
    try {
      ExecutableElement buildVia = viaAnnotatedElement(buildElement);
      TypeName goalType = buildVia.getKind() == CONSTRUCTOR ?
          ClassName.get(buildElement) :
          TypeName.get(buildVia.getReturnType());
      typeValidator.validateBuildType(buildElement);
      AccessType accessType = buildElement.getAnnotation(Build.class).toBuilder()
          ? matchValidatorFactory.buildViaElement(buildVia).buildElement(buildElement).validate()
          : AccessType.NONE;
      MyContext context = createContext(goalType, buildElement, buildVia, accessType);
      myGenerator.generate(context);
    } catch (ValidationException e) {
      e.printMessage(messager);
    }
  }


  private ExecutableElement viaAnnotatedElement(TypeElement buildElement) throws ValidationException {
    ImmutableList.Builder<ExecutableElement> builder = ImmutableList.builder();
    for (ExecutableElement executableElement : concat(constructorsIn(buildElement.getEnclosedElements()),
        methodsIn(buildElement.getEnclosedElements()))) {
      if (executableElement.getAnnotation(Build.Via.class) != null) {
        if (executableElement.getModifiers().contains(PRIVATE)) {
          throw new ValidationException(PRIVATE_METHOD, buildElement);
        }
        if (executableElement.getParameters().isEmpty()) {
          throw new ValidationException(Messages.ErrorMessages.NOT_ENOUGH_PARAMETERS, buildElement);
        }
        builder.add(executableElement);
      }
    }
    ImmutableList<ExecutableElement> elements = builder.build();
    if (elements.isEmpty()) {
      return guessVia(buildElement);
    }
    if (elements.size() > 1) {
      throw new ValidationException(SEVERAL_VIA_ANNOTATIONS, buildElement);
    }
    return getOnlyElement(elements);
  }

  private ExecutableElement guessVia(TypeElement buildElement) throws ValidationException {
    ImmutableList.Builder<ExecutableElement> builder = ImmutableList.builder();
    for (ExecutableElement executableElement : concat(constructorsIn(buildElement.getEnclosedElements()),
        methodsIn(buildElement.getEnclosedElements()))) {
      if (!executableElement.getModifiers().contains(PRIVATE)
          && !executableElement.getParameters().isEmpty()) {
        builder.add(executableElement);
      }
    }
    ImmutableList<ExecutableElement> elements = builder.build();
    if (elements.size() != 1) {
      throw new ValidationException(COULD_NOT_GUESS_VIA, buildElement);
    }
    return getOnlyElement(elements);
  }

}
