package net.zerobuilder.compiler;

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
import java.util.Set;

import static net.zerobuilder.compiler.CodeBlocks.makeParametersCodeBlock;
import static javax.lang.model.element.Modifier.PUBLIC;

final class Target implements GenerationContext {

  enum AccessType {
    FIELDS, AUTOVALUE, GETTERS
  }

  private static final String UPDATER_SUFFIX = "Updater";
  private static final String CONTRACT = "Contract";

  final TypeElement annotatedType;
  final AccessType accessType;
  final ExecutableElement annotatedExecutable;
  final ImmutableList<StepSpec> stepSpecs;

  private Target(TypeElement annotatedType,
                 AccessType accessType,
                 ExecutableElement annotatedExecutable,
                 ImmutableList<StepSpec> stepSpecs) {
    this.annotatedType = annotatedType;
    this.accessType = accessType;
    this.annotatedExecutable = annotatedExecutable;
    this.stepSpecs = stepSpecs;
  }

  static Target target(TypeElement typeElement, ExecutableElement executableElement, AccessType accessType) {
    ImmutableList<StepSpec> specs = specs(typeElement, executableElement);
    return new Target(typeElement, accessType, executableElement, specs);
  }

  private static ImmutableList<StepSpec> specs(TypeElement typeElement, ExecutableElement executableElement) {
    ClassName contractName = generatedClassName(typeElement).nestedClass(CONTRACT);
    ClassName name = ClassName.get(typeElement);
    ImmutableList.Builder<StepSpec> stepSpecsBuilder = ImmutableList.builder();
    for (int i = executableElement.getParameters().size() - 1; i >= 0; i--) {
      VariableElement arg = executableElement.getParameters().get(i);
      ClassName stepName = contractName.nestedClass(Util.upcase(arg.getSimpleName().toString()));
      StepSpec stepSpec = StepSpec.stepSpec(stepName, arg, name);
      stepSpecsBuilder.add(stepSpec);
      name = stepSpec.stepName;
    }
    return stepSpecsBuilder.build().reverse();
  }

  private static ClassName generatedClassName(TypeElement typeElement) {
    ClassName sourceType = ClassName.get(typeElement);
    String simpleName = Joiner.on('_').join(sourceType.simpleNames()) + "Builder";
    return sourceType.topLevelClassName().peerClass(simpleName);
  }

  @Override
  public ClassName generatedTypeName() {
    return generatedClassName(annotatedType);
  }

  ClassName contractName() {
    return generatedTypeName().nestedClass(CONTRACT);
  }

  ClassName contractUpdaterName() {
    return contractName().nestedClass(ClassName.get(annotatedType).simpleName() + UPDATER_SUFFIX);
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
    for (VariableElement arg : annotatedExecutable.getParameters()) {
      builder.add(CodeBlock.of("$L", arg.getSimpleName()));
    }
    return makeParametersCodeBlock(builder.build());
  }

  Set<Modifier> maybeAddPublic(Modifier... minimum) {
    if (annotatedType.getModifiers().contains(PUBLIC)) {
      return Sets.union(ImmutableSet.copyOf(minimum), ImmutableSet.of(PUBLIC));
    }
    return ImmutableSet.copyOf(minimum);
  }

}
