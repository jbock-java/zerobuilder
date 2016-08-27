package isobuilder.compiler;

import com.google.common.base.Joiner;
import com.google.common.collect.ImmutableList;
import com.squareup.javapoet.ClassName;
import com.squareup.javapoet.CodeBlock;
import com.squareup.javapoet.FieldSpec;
import com.squareup.javapoet.MethodSpec;
import com.squareup.javapoet.ParameterSpec;
import com.squareup.javapoet.TypeName;
import com.squareup.javapoet.TypeSpec;

import javax.lang.model.element.ExecutableElement;
import javax.lang.model.element.Name;
import javax.lang.model.element.TypeElement;
import javax.lang.model.element.VariableElement;

import static com.google.auto.common.MoreElements.asType;
import static com.google.auto.common.MoreTypes.asDeclared;
import static com.squareup.javapoet.MethodSpec.constructorBuilder;
import static com.squareup.javapoet.MethodSpec.methodBuilder;
import static com.squareup.javapoet.TypeSpec.interfaceBuilder;
import static isobuilder.compiler.CodeBlocks.makeParametersCodeBlock;
import static isobuilder.compiler.StepSpec.stepSpec;
import static isobuilder.compiler.Util.upcase;
import static javax.lang.model.element.ElementKind.CONSTRUCTOR;
import static javax.lang.model.element.Modifier.ABSTRACT;
import static javax.lang.model.element.Modifier.PRIVATE;
import static javax.lang.model.element.Modifier.PUBLIC;

final class Target {

  private static final String CONTRACT = "Contract";
  private static final String IMPL = "BuilderImpl";
  private static final String UPDATER_SUFFIX = "Updater";

  final ExecutableElement executableElement;
  final ImmutableList<StepSpec> stepSpecs;

  private Target(ExecutableElement executableElement,
                 ImmutableList<StepSpec> stepSpecs) {
    this.executableElement = executableElement;
    this.stepSpecs = stepSpecs;
  }

  static Target target(ExecutableElement executableElement) {
    return new Target(executableElement, specs(executableElement));
  }

  private static ImmutableList<StepSpec> specs(ExecutableElement executableElement) {
    ClassName generatedClassName = generatedClassName(executableElement);
    ClassName contractName = generatedClassName.nestedClass(CONTRACT);
    ClassName name = contractName.nestedClass(goalType(executableElement).simpleName() + UPDATER_SUFFIX);
    ImmutableList.Builder<StepSpec> stepSpecsBuilder = ImmutableList.builder();
    for (int i = executableElement.getParameters().size() - 1; i >= 0; i--) {
      VariableElement arg = executableElement.getParameters().get(i);
      ClassName stepName = contractName.nestedClass(upcase(arg.getSimpleName().toString()));
      StepSpec stepSpec = stepSpec(stepName, arg, name);
      stepSpecsBuilder.add(stepSpec);
      name = stepSpec.stepName;
    }
    return stepSpecsBuilder.build().reverse();
  }

  private static ClassName generatedClassName(ExecutableElement executableElement) {
    ClassName enclosingClass = ClassName.get(asType(executableElement.getEnclosingElement()));
    String returnTypeSimpleName = Joiner.on('_').join(enclosingClass.simpleNames()) + "Builder";
    return enclosingClass.topLevelClassName().peerClass(returnTypeSimpleName);
  }

  private static ClassName goalType(ExecutableElement executableElement) {
    TypeElement type = executableElement.getKind() == CONSTRUCTOR
        ? asType(executableElement.getEnclosingElement())
        : asType(asDeclared(executableElement.getReturnType()).asElement());
    return ClassName.get(type);
  }

  ClassName goalType() {
    return goalType(executableElement);
  }

  ClassName generatedClassName() {
    return generatedClassName(executableElement);
  }

  ClassName contractName() {
    return generatedClassName().nestedClass(CONTRACT);
  }

  ClassName updaterName() {
    return contractName().nestedClass(goalType(executableElement).simpleName() + UPDATER_SUFFIX);
  }

  Impl impl() {
    return new Impl();
  }

  Contract contract() {
    return new Contract();
  }

  final class Contract {

    ImmutableList<ClassName> interfaceNames() {
      ImmutableList.Builder<ClassName> specs = ImmutableList.builder();
      specs.add(updaterName());
      for (int i = 1; i < stepSpecs.size(); i++) {
        specs.add(stepSpecs.get(i).stepName);
      }
      return specs.build();
    }

    ImmutableList<TypeSpec> interfaces() {
      ImmutableList.Builder<TypeSpec> specs = ImmutableList.builder();
      specs.add(updaterInterface());
      for (int i = 1; i < stepSpecs.size(); i++) {
        specs.add(stepSpecs.get(i).asInterface());
      }
      return specs.build();
    }

    private TypeSpec updaterInterface() {
      MethodSpec buildMethod = methodBuilder("build")
          .returns(executableElement.getKind() == CONSTRUCTOR
              ? ClassName.get(asType(executableElement.getEnclosingElement()))
              : TypeName.get(executableElement.getReturnType()))
          .addModifiers(PUBLIC, ABSTRACT)
          .build();
      return interfaceBuilder(updaterName())
          .addMethod(buildMethod)
          .addMethods(updateMethods())
          .addModifiers(PUBLIC)
          .build();
    }

    private ImmutableList<MethodSpec> updateMethods() {
      ImmutableList.Builder<MethodSpec> builder = ImmutableList.builder();
      for (StepSpec spec : stepSpecs) {
        builder.add(methodBuilder("update" + upcase(spec.argument.getSimpleName().toString()))
            .returns(updaterName())
            .addParameter(spec.asParameter())
            .addModifiers(PUBLIC, ABSTRACT)
            .build());
      }
      return builder.build();
    }

  }

  final class Impl {

    ClassName name() {
      return generatedClassName().nestedClass(IMPL);
    }

    private CodeBlock factoryCallArgs() {
      ImmutableList.Builder<CodeBlock> builder = ImmutableList.builder();
      for (VariableElement arg : executableElement.getParameters()) {
        builder.add(CodeBlock.of("$L", arg.getSimpleName()));
      }
      return makeParametersCodeBlock(builder.build());
    }

    ImmutableList<FieldSpec> fields() {
      ImmutableList.Builder<FieldSpec> builder = ImmutableList.builder();
      for (StepSpec stepSpec : stepSpecs) {
        String name = stepSpec.argument.getSimpleName().toString();
        builder.add(FieldSpec.builder(TypeName.get(stepSpec.argument.asType()), name, PRIVATE).build());
      }
      return builder.build();
    }

    MethodSpec constructor() {
      ParameterSpec parameter = stepSpecs.get(0).asParameter();
      return constructorBuilder()
          .addParameter(parameter)
          .addStatement("this.$N = $N", parameter.name, parameter.name)
          .addModifiers(PRIVATE)
          .build();
    }

    ImmutableList<MethodSpec> updaters() {
      ImmutableList.Builder<MethodSpec> builder = ImmutableList.builder();
      for (StepSpec stepSpec : stepSpecs) {
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
      for (int i = 1; i < stepSpecs.size(); i++) {
        StepSpec stepSpec = stepSpecs.get(i);
        ParameterSpec parameter = stepSpec.asParameter();
        builder.add(methodBuilder(parameter.name)
            .addAnnotation(Override.class)
            .returns(stepSpec.returnType)
            .addParameter(parameter)
            .addStatement("this.$N = $N", parameter.name, parameter.name)
            .addStatement("return this")
            .addModifiers(PUBLIC)
            .build());
      }
      return builder.build();
    }

    MethodSpec buildMethod() {
      ClassName enclosing = ClassName.get(asType(executableElement.getEnclosingElement()));
      MethodSpec.Builder builder = methodBuilder("build")
          .addAnnotation(Override.class)
          .addModifiers(PUBLIC)
          .returns(executableElement.getKind() == CONSTRUCTOR
              ? enclosing
              : TypeName.get(executableElement.getReturnType()));
      Name simpleName = executableElement.getSimpleName();
      return (executableElement.getKind() == CONSTRUCTOR
          ? builder.addStatement("return new $T($L)", enclosing, factoryCallArgs())
          : builder.addStatement("return $T.$N($L)", enclosing, simpleName, factoryCallArgs()))
          .build();
    }

  }

}
