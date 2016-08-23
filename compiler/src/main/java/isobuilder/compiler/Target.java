package isobuilder.compiler;

import com.google.auto.value.AutoValue;
import com.google.common.base.Joiner;
import com.google.common.collect.ImmutableList;
import com.squareup.javapoet.ClassName;

import javax.lang.model.element.ExecutableElement;
import javax.lang.model.element.TypeElement;
import javax.lang.model.element.VariableElement;
import javax.lang.model.type.DeclaredType;

import static com.google.auto.common.MoreElements.asType;
import static com.google.auto.common.MoreTypes.asDeclared;
import static isobuilder.compiler.Util.upcase;

@AutoValue
abstract class Target {

  abstract ExecutableElement executableElement();
  abstract ImmutableList<StepSpec> stepSpecs();
  abstract ClassName generatedClassName();
  abstract ClassName contractName();
  abstract ClassName updaterName();

  static Target target(ExecutableElement executableElement) {
    ImmutableList.Builder<StepSpec> stepSpecsBuilder = ImmutableList.builder();
    ClassName generatedClassName = generatedClassName(executableElement);
    ClassName contractName = generatedClassName.nestedClass("Contract");
    String simpleReturnTypeName = returnTypeName(executableElement).simpleName();
    ClassName updaterName = contractName.nestedClass(simpleReturnTypeName + "Updater");
    for (VariableElement arg : executableElement.getParameters()) {
      ClassName stepName = contractName.nestedClass(simpleReturnTypeName + upcase(arg.getSimpleName().toString()));
      stepSpecsBuilder.add(new StepSpec(stepName, arg));
    }
    return new AutoValue_Target(executableElement, stepSpecsBuilder.build(), generatedClassName, contractName, updaterName);
  }

  private static ClassName generatedClassName(ExecutableElement executableElement) {
    ClassName enclosingClass = ClassName.get(asType(executableElement.getEnclosingElement()));
    String returnTypeSimpleName = Joiner.on('_').join(enclosingClass.simpleNames()) + "_IsoBuilder";
    return enclosingClass.topLevelClassName().peerClass(returnTypeSimpleName);
  }

  private static ClassName returnTypeName(ExecutableElement executableElement) {
    DeclaredType returnType = asDeclared(executableElement.getReturnType());
    TypeElement typeElement = asType(returnType.asElement());
    return ClassName.get(typeElement);
  }

  final ClassName returnTypeName() {
    return returnTypeName(executableElement());
  }

  final StepSpec stepSpec(int i) {
    return stepSpecs().get(i);
  }


}
