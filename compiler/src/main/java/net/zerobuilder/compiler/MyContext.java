package net.zerobuilder.compiler;

import com.google.common.base.Joiner;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableSet;
import com.squareup.javapoet.ClassName;
import com.squareup.javapoet.CodeBlock;

import javax.lang.model.element.ExecutableElement;
import javax.lang.model.element.Modifier;
import javax.lang.model.element.TypeElement;
import javax.lang.model.element.VariableElement;
import java.util.Set;

import static javax.lang.model.element.Modifier.PUBLIC;
import static net.zerobuilder.compiler.MyContext.AccessType.NONE;
import static net.zerobuilder.compiler.Util.joinCodeBlocks;

final class MyContext implements GenerationContext {

  enum AccessType {
    FIELDS, AUTOVALUE, GETTERS, NONE
  }

  private static final String UPDATER_SUFFIX = "Updater";
  private static final String CONTRACT = "Contract";

  final ClassName goalType;
  final TypeElement buildElement;
  final AccessType accessType;
  final ExecutableElement buildVia;
  final ImmutableList<StepSpec> stepSpecs;

  private MyContext(ClassName goalType, TypeElement buildElement,
                    AccessType accessType,
                    ExecutableElement buildVia,
                    ImmutableList<StepSpec> stepSpecs) {
    this.goalType = goalType;
    this.buildElement = buildElement;
    this.accessType = accessType;
    this.buildVia = buildVia;
    this.stepSpecs = stepSpecs;
  }

  static MyContext createContext(ClassName goalType, TypeElement buildElement, ExecutableElement buildVia, AccessType accessType) {
    ImmutableList<StepSpec> specs = specs(buildElement, goalType, buildVia);
    return new MyContext(goalType, buildElement, accessType, buildVia, specs);
  }

  private static ImmutableList<StepSpec> specs(TypeElement typeElement, ClassName goalType, ExecutableElement executableElement) {
    ClassName contractName = generatedClassName(typeElement).nestedClass(CONTRACT);
    ImmutableList.Builder<StepSpec> stepSpecsBuilder = ImmutableList.builder();
    for (int i = executableElement.getParameters().size() - 1; i >= 0; i--) {
      VariableElement arg = executableElement.getParameters().get(i);
      ClassName stepName = contractName.nestedClass(Util.upcase(arg.getSimpleName().toString()));
      StepSpec stepSpec = StepSpec.stepSpec(stepName, arg, goalType);
      stepSpecsBuilder.add(stepSpec);
      goalType = stepSpec.stepName;
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
    return generatedClassName(buildElement);
  }

  ClassName contractName() {
    return generatedTypeName().nestedClass(CONTRACT);
  }

  ClassName contractUpdaterName() {
    return contractName().nestedClass(ClassName.get(buildElement).simpleName() + UPDATER_SUFFIX);
  }

  StepsContext stepsContext() {
    return new StepsContext(this);
  }

  UpdaterContext updaterContext() {
    return new UpdaterContext(this);
  }

  ContractContext contractContext() {
    return new ContractContext(this);
  }

  CodeBlock factoryCallArgs() {
    ImmutableList.Builder<CodeBlock> builder = ImmutableList.builder();
    for (VariableElement arg : buildVia.getParameters()) {
      builder.add(CodeBlock.of("$L", arg.getSimpleName()));
    }
    return joinCodeBlocks(builder.build(), ", ");
  }

  Set<Modifier> maybeAddPublic(Modifier... modifiers) {
    ImmutableSet<Modifier> modifierSet = ImmutableSet.copyOf(modifiers);
    if (buildVia.getModifiers().contains(PUBLIC)
        && !modifierSet.contains(PUBLIC)) {
      return new ImmutableSet.Builder<Modifier>().addAll(modifierSet).add(PUBLIC).build();
    }
    return modifierSet;
  }

  boolean toBuilder() {
    return accessType != NONE;
  }

}
