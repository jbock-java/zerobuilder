package isobuilder.compiler;

import com.google.common.base.Joiner;
import com.google.common.collect.ImmutableList;
import com.squareup.javapoet.ClassName;

import javax.lang.model.element.ExecutableElement;
import javax.lang.model.element.TypeElement;
import javax.lang.model.element.VariableElement;
import javax.lang.model.type.DeclaredType;

import static com.google.auto.common.MoreElements.asType;
import static com.google.auto.common.MoreTypes.asDeclared;
import static isobuilder.compiler.ContractGenerator.upcase;

final class Target {

  private final ExecutableElement executableElement;
  private final ImmutableList<StepSpec> stepSpecs;
  private final ClassName contractName;

  Target(ExecutableElement executableElement, ImmutableList<StepSpec> stepSpecs, ClassName contractName) {
    this.executableElement = executableElement;
    this.stepSpecs = stepSpecs;
    this.contractName = contractName;
  }

  static Target target(ExecutableElement executableElement) {
    ImmutableList.Builder<StepSpec> stepSpecsBuilder = ImmutableList.builder();
    ClassName contractName = nameGeneratedType(executableElement, "BuilderContract");
    for (VariableElement arg: executableElement.getParameters()) {
      ClassName stepName = contractName.nestedClass(upcase(arg.getSimpleName() + "Step"));
      stepSpecsBuilder.add(new StepSpec(stepName, arg));
    }
    return new Target(executableElement, stepSpecsBuilder.build(), contractName);
  }

  static ClassName nameGeneratedType(ExecutableElement executableElement, String suffix) {
    ClassName enclosingClass = ClassName.get(asType(executableElement.getEnclosingElement()));
    String returnTypeSimpleName = Joiner.on('_').join(returnTypeName(executableElement).simpleNames()) + suffix;
    return enclosingClass.topLevelClassName().peerClass(returnTypeSimpleName);
  }

  private static ClassName returnTypeName(ExecutableElement executableElement) {
    DeclaredType returnType = asDeclared(executableElement.getReturnType());
    TypeElement typeElement = asType(returnType.asElement());
    return ClassName.get(typeElement);
  }

  ClassName returnTypeName() {
    return returnTypeName(executableElement);
  }

  ImmutableList<StepSpec> stepSpecs() {
    return stepSpecs;
  }

  ClassName contractName() {
    return contractName;
  }

  ExecutableElement getExecutableElement() {
    return executableElement;
  }

}
