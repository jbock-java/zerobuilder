package isobuilder.compiler;

import com.google.common.base.Joiner;
import com.squareup.javapoet.ClassName;

import javax.lang.model.element.ExecutableElement;
import javax.lang.model.element.TypeElement;
import javax.lang.model.type.DeclaredType;

import static com.google.auto.common.MoreElements.asType;
import static com.google.auto.common.MoreTypes.asDeclared;

final class Target {

  private final ExecutableElement executableElement;

  Target(ExecutableElement executableElement) {
    this.executableElement = executableElement;
  }

  static Target target(ExecutableElement executableElement) {
    return new Target(executableElement);
  }

  ClassName nameGeneratedType(String suffix) {
    ClassName enclosingClass = ClassName.get(asType(executableElement.getEnclosingElement()));
    String returnTypeSimpleName = Joiner.on('_').join(returnTypeName().simpleNames()) + suffix;
    return enclosingClass.topLevelClassName().peerClass(returnTypeSimpleName);

  }

  ClassName returnTypeName() {
    DeclaredType returnType = asDeclared(executableElement.getReturnType());
    TypeElement typeElement = asType(returnType.asElement());
    return ClassName.get(typeElement);
  }

  public ExecutableElement getExecutableElement() {
    return executableElement;
  }

}
