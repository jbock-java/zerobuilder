package isobuilder.compiler;

import com.google.common.base.Joiner;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Sets;
import com.squareup.javapoet.ClassName;
import com.squareup.javapoet.CodeBlock;

import javax.lang.model.element.ExecutableElement;
import javax.lang.model.element.Modifier;
import javax.lang.model.element.TypeElement;
import javax.lang.model.element.VariableElement;
import java.util.Arrays;
import java.util.EnumSet;
import java.util.Set;

import static isobuilder.compiler.CodeBlocks.makeParametersCodeBlock;
import static isobuilder.compiler.StepSpec.stepSpec;
import static isobuilder.compiler.Util.upcase;
import static javax.lang.model.element.Modifier.FINAL;
import static javax.lang.model.element.Modifier.PUBLIC;
import static javax.lang.model.element.Modifier.STATIC;

final class Target {

  private static final String CONTRACT = "Contract";
  static final String UPDATER_IMPL = "UpdaterImpl";
  static final String STEPS_IMPL = "StepsImpl";
  private static final String UPDATER_SUFFIX = "Updater";

  final TypeElement typeElement;
  final ExecutableElement executableElement;
  final ImmutableList<StepSpec> stepSpecs;

  private Target(TypeElement typeElement,
                 ExecutableElement executableElement,
                 ImmutableList<StepSpec> stepSpecs) {
    this.typeElement = typeElement;
    this.executableElement = executableElement;
    this.stepSpecs = stepSpecs;
  }

  static Target target(TypeElement typeElement, ExecutableElement executableElement) {
    return new Target(typeElement, executableElement, specs(typeElement, executableElement));
  }

  private static ImmutableList<StepSpec> specs(TypeElement typeElement, ExecutableElement executableElement) {
    ClassName contractName = generatedClassName(typeElement).nestedClass(CONTRACT);
    ClassName name = ClassName.get(typeElement);
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

  private static ClassName generatedClassName(TypeElement typeElement) {
    ClassName enclosingClass = ClassName.get(typeElement);
    String returnTypeSimpleName = Joiner.on('_').join(enclosingClass.simpleNames()) + "Builder";
    return enclosingClass.topLevelClassName().peerClass(returnTypeSimpleName);
  }

  ClassName generatedClassName() {
    return generatedClassName(typeElement);
  }

  ClassName contractName() {
    return generatedClassName().nestedClass(CONTRACT);
  }

  ClassName contractUpdaterName() {
    return contractName().nestedClass(ClassName.get(typeElement).simpleName() + UPDATER_SUFFIX);
  }

  StepsImpl stepsImpl() {
    return new StepsImpl(this);
  }

  UpdaterImpl updaterImpl() {
    return new UpdaterImpl(this);
  }

  Contract contract() {
    return new Contract(this);
  }

  CodeBlock factoryCallArgs() {
    ImmutableList.Builder<CodeBlock> builder = ImmutableList.builder();
    for (VariableElement arg : executableElement.getParameters()) {
      builder.add(CodeBlock.of("$L", arg.getSimpleName()));
    }
    return makeParametersCodeBlock(builder.build());
  }

  Set<Modifier> methodModifiers(Set<Modifier> minimum) {
    if (typeElement.getModifiers().contains(PUBLIC)) {
      return Sets.union(ImmutableSet.copyOf(minimum), ImmutableSet.of(PUBLIC));
    }
    return ImmutableSet.copyOf(minimum);
  }

  Modifier[] typeModifiers(Set<Modifier> minimum) {
    if (typeElement.getModifiers().contains(PUBLIC)) {
      if (!minimum.contains(PUBLIC)) {
        ImmutableList.Builder<Object> builder = ImmutableList.builder();
        return builder.addAll(minimum).add(PUBLIC).build().toArray(new Modifier[0]);
      }
    }
    return minimum.toArray(new Modifier[0]);
  }

}
