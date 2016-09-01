package net.zerobuilder.compiler;

import com.google.common.base.Function;
import com.google.common.base.Joiner;
import com.google.common.base.Optional;
import com.google.common.collect.FluentIterable;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Lists;
import com.squareup.javapoet.ClassName;
import com.squareup.javapoet.CodeBlock;
import com.squareup.javapoet.TypeName;

import javax.lang.model.element.ExecutableElement;
import javax.lang.model.element.Modifier;
import javax.lang.model.element.TypeElement;
import javax.lang.model.element.VariableElement;
import javax.lang.model.type.TypeMirror;
import java.util.List;
import java.util.Set;

import static javax.lang.model.element.ElementKind.METHOD;
import static javax.lang.model.element.Modifier.PUBLIC;
import static javax.lang.model.element.Modifier.STATIC;
import static net.zerobuilder.compiler.MyContext.AccessType.NONE;
import static net.zerobuilder.compiler.Util.joinCodeBlocks;

final class MyContext implements GenerationContext {

  enum AccessType {
    FIELDS, AUTOVALUE, GETTERS, NONE
  }

  private static final String UPDATER_SUFFIX = "Updater";
  private static final String CONTRACT = "Contract";
  private static final String STEPS_IMPL = "StepsImpl";

  final TypeName goalType;
  final TypeElement buildElement;
  final AccessType accessType;
  final ExecutableElement buildVia;
  final ImmutableList<StepSpec> stepSpecs;

  private MyContext(TypeName goalType, TypeElement buildElement,
                    AccessType accessType,
                    ExecutableElement buildVia,
                    ImmutableList<StepSpec> stepSpecs) {
    this.goalType = goalType;
    this.buildElement = buildElement;
    this.accessType = accessType;
    this.buildVia = buildVia;
    this.stepSpecs = stepSpecs;
  }

  static MyContext createContext(TypeName goalType, TypeElement buildElement, ExecutableElement buildVia, AccessType accessType) {
    ImmutableList<StepSpec> specs = specs(buildElement, goalType, buildVia);
    return new MyContext(goalType, buildElement, accessType, buildVia, specs);
  }

  private static ImmutableList<StepSpec> specs(TypeElement typeElement, TypeName goalType, ExecutableElement executableElement) {
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

  ImmutableList<ClassName> stepInterfaceNames() {
    ImmutableList.Builder<ClassName> specs = ImmutableList.builder();
    for (StepSpec spec : stepSpecs) {
      specs.add(spec.stepName);
    }
    return specs.build();
  }

  ImmutableList<TypeName> thrownTypes() {
    return FluentIterable.from(buildVia.getThrownTypes()).transform(new Function<TypeMirror, TypeName>() {
      @Override
      public TypeName apply(TypeMirror thrownType) {
        return TypeName.get(thrownType);
      }
    }).toList();
  }

  Optional<ClassName> receiver() {
    return buildVia.getKind() == METHOD && !buildVia.getModifiers().contains(STATIC)
        ? Optional.of(ClassName.get(buildElement))
        : Optional.<ClassName>absent();
  }

  @Override
  public ClassName generatedTypeName() {
    return generatedClassName(buildElement);
  }

  ClassName contractName() {
    return generatedTypeName().nestedClass(CONTRACT);
  }

  ClassName stepsImplTypeName() {
    return generatedTypeName().nestedClass(STEPS_IMPL);
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
