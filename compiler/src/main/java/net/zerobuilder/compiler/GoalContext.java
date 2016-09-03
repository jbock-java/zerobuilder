package net.zerobuilder.compiler;

import com.google.common.base.Function;
import com.google.common.base.Optional;
import com.google.common.collect.FluentIterable;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableSet;
import com.squareup.javapoet.ClassName;
import com.squareup.javapoet.CodeBlock;
import com.squareup.javapoet.TypeName;
import com.squareup.javapoet.TypeSpec;
import net.zerobuilder.compiler.MatchValidator.ProjectionInfo;

import javax.lang.model.element.ExecutableElement;
import javax.lang.model.element.Modifier;
import javax.lang.model.element.VariableElement;
import javax.lang.model.type.TypeMirror;
import java.util.Set;

import static com.google.common.base.Optional.presentInstances;
import static com.google.common.collect.ImmutableList.of;
import static com.google.common.collect.Iterables.toArray;
import static com.squareup.javapoet.TypeSpec.classBuilder;
import static javax.lang.model.element.ElementKind.METHOD;
import static javax.lang.model.element.Modifier.FINAL;
import static javax.lang.model.element.Modifier.PUBLIC;
import static javax.lang.model.element.Modifier.STATIC;
import static net.zerobuilder.compiler.Util.joinCodeBlocks;
import static net.zerobuilder.compiler.Util.upcase;

final class GoalContext {

  private static final String UPDATER_SUFFIX = "Updater";
  private static final String CONTRACT = "Contract";
  private static final String STEPS_IMPL = "StepsImpl";

  /**
   * return type of {@link #goal}
   */
  final TypeName goalType;

  /**
   * container
   */
  final ClassName builderType;

  final BuildConfig config;

  /**
   * the element carrying the {@link net.zerobuilder.Build.Goal} annotation
   */
  final ExecutableElement goal;

  /**
   * arguments of the {@link #goal}
   */
  final ImmutableList<StepSpec> stepSpecs;

  private GoalContext(TypeName goalType, ClassName builderType, BuildConfig config,
                      ExecutableElement goal,
                      ImmutableList<StepSpec> stepSpecs) {
    this.goalType = goalType;
    this.builderType = builderType;
    this.config = config;
    this.goal = goal;
    this.stepSpecs = stepSpecs;
  }

  static GoalContext createGoalContext(TypeName goalType, BuildConfig config,
                                       ImmutableList<ProjectionInfo> projectionInfos,
                                       ExecutableElement goal) {
    String builderTypeName = ((ClassName) goalType.box()).simpleName() + "Builder";
    ClassName builderType = config.generatedType.nestedClass(builderTypeName);
    ClassName contractType = builderType.nestedClass(CONTRACT);
    ImmutableList<StepSpec> specs = specs(contractType, goalType, projectionInfos);
    return new GoalContext(goalType, builderType, config, goal, specs);
  }

  private static ImmutableList<StepSpec> specs(ClassName contractType, TypeName goalType,
                                               ImmutableList<ProjectionInfo> goal) {
    ImmutableList.Builder<StepSpec> stepSpecsBuilder = ImmutableList.builder();
    for (int i = goal.size() - 1; i >= 0; i--) {
      ProjectionInfo arg = goal.get(i);
      ClassName stepName = contractType.nestedClass(
          upcase(arg.parameter.getSimpleName().toString()));
      StepSpec stepSpec = StepSpec.stepSpec(stepName, arg, goalType);
      stepSpecsBuilder.add(stepSpec);
      goalType = stepSpec.stepContractType;
    }
    return stepSpecsBuilder.build().reverse();
  }

  ImmutableList<ClassName> stepInterfaceNames() {
    ImmutableList.Builder<ClassName> specs = ImmutableList.builder();
    for (StepSpec spec : stepSpecs) {
      specs.add(spec.stepContractType);
    }
    return specs.build();
  }

  ImmutableList<TypeName> thrownTypes() {
    return FluentIterable.from(goal.getThrownTypes())
        .transform(new Function<TypeMirror, TypeName>() {
          @Override
          public TypeName apply(TypeMirror thrownType) {
            return TypeName.get(thrownType);
          }
        })
        .toList();
  }

  Optional<ClassName> receiverType() {
    return goal.getKind() == METHOD && !goal.getModifiers().contains(STATIC)
        ? Optional.of(config.annotatedType)
        : Optional.<ClassName>absent();
  }

  TypeSpec builderImpl() {
    return classBuilder(builderType)
        .addTypes(presentInstances(of(updaterContext().buildUpdaterImpl())))
        .addType(stepsContext().buildStepsImpl())
        .addType(contractContext().buildContract())
        .addModifiers(toArray(maybeAddPublic(FINAL, STATIC), Modifier.class))
        .build();
  }

  ClassName contractName() {
    return builderType.nestedClass(CONTRACT);
  }

  ClassName stepsImplTypeName() {
    return builderType.nestedClass(STEPS_IMPL);
  }

  ClassName contractUpdaterName() {
    return contractName()
        .nestedClass(((ClassName) goalType.box()).simpleName() + UPDATER_SUFFIX);
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

  CodeBlock goalParameters() {
    ImmutableList.Builder<CodeBlock> builder = ImmutableList.builder();
    for (VariableElement arg : goal.getParameters()) {
      builder.add(CodeBlock.of("$L", arg.getSimpleName()));
    }
    return joinCodeBlocks(builder.build(), ", ");
  }

  String goalTypeSimpleName() {
    return ((ClassName) goalType.box()).simpleName();
  }

  Set<Modifier> maybeAddPublic(Modifier... modifiers) {
    return maybeAddPublic(goal.getModifiers().contains(PUBLIC), modifiers);
  }

  static Set<Modifier> maybeAddPublic(boolean add, Modifier... modifiers) {
    ImmutableSet<Modifier> modifierSet = ImmutableSet.copyOf(modifiers);
    if (add && !modifierSet.contains(PUBLIC)) {
      return new ImmutableSet.Builder<Modifier>().addAll(modifierSet).add(PUBLIC).build();
    }
    return modifierSet;
  }

}
