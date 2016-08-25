package isobuilder.compiler;

import com.google.auto.value.AutoValue;
import com.google.common.base.Joiner;
import com.google.common.collect.ImmutableList;
import com.squareup.javapoet.*;

import javax.lang.model.element.ExecutableElement;
import javax.lang.model.element.TypeElement;
import javax.lang.model.element.VariableElement;
import javax.lang.model.type.DeclaredType;
import java.util.Stack;

import static com.google.auto.common.MoreElements.asType;
import static com.google.auto.common.MoreTypes.asDeclared;
import static com.squareup.javapoet.MethodSpec.constructorBuilder;
import static com.squareup.javapoet.MethodSpec.methodBuilder;
import static com.squareup.javapoet.TypeSpec.interfaceBuilder;
import static isobuilder.compiler.CodeBlocks.makeParametersCodeBlock;
import static isobuilder.compiler.Util.upcase;
import static javax.lang.model.element.Modifier.*;

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
    String returnTypeSimpleName = Joiner.on('_').join(enclosingClass.simpleNames()) + "Builder";
    return enclosingClass.topLevelClassName().peerClass(returnTypeSimpleName);
  }

  private static ClassName returnTypeName(ExecutableElement executableElement) {
    DeclaredType returnType = asDeclared(executableElement.getReturnType());
    TypeElement typeElement = asType(returnType.asElement());
    return ClassName.get(typeElement);
  }

  final ImmutableList<TypeSpec> contractInterfaces() {
    ImmutableList.Builder<TypeSpec> specs = ImmutableList.builder();
    specs.add(updaterSpec());
    for (int i = 1; i < stepSpecs().size(); i++) {
      specs.add(stepSpecs().get(i).typeSpec());
    }
    return specs.build();
  }

  final ImmutableList<ClassName> contractInterfaceNames() {
    ImmutableList.Builder<ClassName> specs = ImmutableList.builder();
    specs.add(updaterName());
    for (int i = 1; i < stepSpecs().size(); i++) {
      specs.add(stepSpecs().get(i).stepName());
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

  final Impl impl() {
    return new Impl();
  }

  final class Impl {

    private CodeBlock factoryCallArgs() {
      ImmutableList.Builder<CodeBlock> builder = ImmutableList.builder();
      for (VariableElement arg : executableElement().getParameters()) {
        builder.add(CodeBlock.of("$L", arg.getSimpleName()));
      }
      return makeParametersCodeBlock(builder.build());
    }

    ImmutableList<FieldSpec> fields() {
      ImmutableList.Builder<FieldSpec> builder = ImmutableList.builder();
      for (StepSpec stepSpec : stepSpecs()) {
        String name = stepSpec.argument().getSimpleName().toString();
        builder.add(FieldSpec.builder(TypeName.get(stepSpec.argument().asType()), name, PRIVATE).build());
      }
      return builder.build();
    }

    MethodSpec constructor() {
      ParameterSpec parameter = stepSpecs().get(0).asParameter();
      return constructorBuilder()
          .addParameter(parameter)
          .addStatement("this.$N = $N", parameter.name, parameter.name)
          .addModifiers(PRIVATE)
          .build();
    }

    ImmutableList<MethodSpec> updaters() {
      ImmutableList.Builder<MethodSpec> builder = ImmutableList.builder();
      for (StepSpec stepSpec : stepSpecs()) {
        ParameterSpec parameter = stepSpec.asParameter();
        builder.add(methodBuilder("update" + upcase(parameter.name))
            .addAnnotation(Override.class)
            .returns(updaterName())
            .addParameter(parameter)
            .addStatement("this.$N = $N", parameter.name, parameter.name)
            .addStatement("return this")
            .addModifiers(PUBLIC)
            .build());
      }
      return builder.build();
    }

    ImmutableList<MethodSpec> steppers() {
      ImmutableList.Builder<MethodSpec> builder = ImmutableList.builder();
      for (int i = 1; i < stepSpecs().size(); i++) {
        StepSpec stepSpec = stepSpecs().get(i);
        ParameterSpec parameter = stepSpec.asParameter();
        builder.add(methodBuilder(parameter.name)
            .addAnnotation(Override.class)
            .returns(stepSpec.returnType())
            .addParameter(parameter)
            .addStatement("this.$N = $N", parameter.name, parameter.name)
            .addStatement("return this")
            .addModifiers(PUBLIC)
            .build());
      }
      return builder.build();
    }

    MethodSpec buildMethod() {
      return methodBuilder("build")
          .addAnnotation(Override.class)
          .returns(TypeName.get(executableElement().getReturnType()))
          .addStatement("return $T.$N($L)",
              ClassName.get(asType(executableElement().getEnclosingElement())),
              executableElement().getSimpleName(),
              factoryCallArgs())
          .addModifiers(PUBLIC)
          .build();
    }

  }

}
