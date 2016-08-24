package isobuilder.compiler;

import com.google.auto.value.AutoValue;
import com.google.common.base.Joiner;
import com.google.common.collect.ImmutableList;
import com.squareup.javapoet.ClassName;
import com.squareup.javapoet.CodeBlock;
import com.squareup.javapoet.TypeName;
import com.squareup.javapoet.TypeSpec;

import javax.lang.model.element.ExecutableElement;
import javax.lang.model.element.TypeElement;
import javax.lang.model.element.VariableElement;
import javax.lang.model.type.DeclaredType;

import java.util.Stack;

import static com.google.auto.common.MoreElements.asType;
import static com.google.auto.common.MoreTypes.asDeclared;
import static com.squareup.javapoet.MethodSpec.methodBuilder;
import static com.squareup.javapoet.TypeSpec.interfaceBuilder;
import static isobuilder.compiler.CodeBlocks.makeParametersCodeBlock;
import static isobuilder.compiler.Util.upcase;
import static javax.lang.model.element.Modifier.ABSTRACT;
import static javax.lang.model.element.Modifier.PUBLIC;

@AutoValue
abstract class Target {

  abstract ExecutableElement executableElement();
  abstract ImmutableList<StepSpec> stepSpecs();
  abstract ClassName generatedClassName();
  abstract ClassName contractName();
  abstract ClassName implName();
  abstract ClassName updaterName();

  static Target target(ExecutableElement executableElement) {
    ImmutableList.Builder<StepSpec> stepSpecsBuilder = ImmutableList.builder();
    ClassName generatedClassName = generatedClassName(executableElement);
    ClassName contractName = generatedClassName.nestedClass("Contract");
    ClassName implName = generatedClassName.nestedClass("BuilderImpl");
    String simpleReturnTypeName = returnTypeName(executableElement).simpleName();
    ClassName updaterName = contractName.nestedClass(simpleReturnTypeName + "Updater");
    Stack<ClassName> names = new Stack<>();
    names.push(updaterName);
    for (int i = executableElement.getParameters().size() - 1; i >= 0; i--) {
      VariableElement arg = executableElement.getParameters().get(i);
      ClassName stepName = contractName.nestedClass(simpleReturnTypeName + upcase(arg.getSimpleName().toString()));
      StepSpec stepSpec = StepSpec.stepSpec(stepName, arg, names.peek());
      stepSpecsBuilder.add(stepSpec);
      names.push(stepSpec.stepName());
    }
    return new AutoValue_Target(executableElement,
        stepSpecsBuilder.build().reverse(),
        generatedClassName,
        contractName,
        implName,
        updaterName);
  }

  private static ClassName generatedClassName(ExecutableElement executableElement) {
    ClassName enclosingClass = ClassName.get(asType(executableElement.getEnclosingElement()));
    String returnTypeSimpleName = "IsoBuilder_" + Joiner.on('_').join(enclosingClass.simpleNames());
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

  final ImmutableList<TypeSpec> contractInterfaces() {
    ImmutableList.Builder<TypeSpec> specs = ImmutableList.builder();
    specs.add(updaterSpec());
    for (StepSpec stepSpec : stepSpecs()) {
      specs.add(stepSpec.typeSpec());
    }
    return specs.build();
  }

  final ImmutableList<ClassName> contractInterfaceNames() {
    ImmutableList.Builder<ClassName> specs = ImmutableList.builder();
    specs.add(updaterName());
    for (StepSpec stepSpec : stepSpecs()) {
      specs.add(stepSpec.stepName());
    }
    return specs.build();
  }

  private final TypeSpec updaterSpec() {
    TypeSpec.Builder updater = interfaceBuilder(updaterName());
    updater.addMethod(methodBuilder("build")
        .returns(TypeName.get(executableElement().getReturnType()))
        .addModifiers(PUBLIC, ABSTRACT)
        .build());
    for (VariableElement arg : executableElement().getParameters()) {
      updater.addMethod(methodBuilder("update" + upcase(arg.getSimpleName().toString()))
          .returns(updaterName())
          .addParameter(TypeName.get(arg.asType()), arg.getSimpleName().toString())
          .addModifiers(PUBLIC, ABSTRACT)
          .build());
    }
    return updater.build();
  }

  CodeBlock factoryCallArgs() {
    ImmutableList.Builder<CodeBlock> builder = ImmutableList.builder();
    for (VariableElement arg : executableElement().getParameters()) {
      builder.add(CodeBlock.of("$L", arg.getSimpleName()));
    }
    return makeParametersCodeBlock(builder.build());
  }


}
