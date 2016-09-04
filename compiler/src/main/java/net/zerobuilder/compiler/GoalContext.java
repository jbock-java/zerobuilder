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
import net.zerobuilder.Build;
import net.zerobuilder.compiler.ToBuilderValidator.ProjectionInfo;

import javax.lang.model.element.ExecutableElement;
import javax.lang.model.element.Modifier;
import javax.lang.model.element.VariableElement;
import javax.lang.model.type.TypeMirror;
import java.util.Set;

import static com.google.common.base.Optional.presentInstances;
import static com.google.common.base.Strings.isNullOrEmpty;
import static com.google.common.collect.ImmutableList.of;
import static com.google.common.collect.Iterables.toArray;
import static com.squareup.javapoet.TypeSpec.classBuilder;
import static javax.lang.model.element.ElementKind.METHOD;
import static javax.lang.model.element.Modifier.FINAL;
import static javax.lang.model.element.Modifier.PUBLIC;
import static javax.lang.model.element.Modifier.STATIC;
import static net.zerobuilder.compiler.GoalContext.SharedGoalContext.goalName;
import static net.zerobuilder.compiler.Utilities.joinCodeBlocks;
import static net.zerobuilder.compiler.Utilities.upcase;

final class GoalContext {

  private static final String UPDATER_SUFFIX = "Updater";
  private static final String CONTRACT = "Contract";
  private static final String STEPS_IMPL = "StepsImpl";

  final SharedGoalContext innerContext;
  final StepsContext stepsContext;
  final ContractContext contractContext;
  final UpdaterContext updaterContext;

  private GoalContext(SharedGoalContext innerContext,
                      StepsContext stepsContext,
                      ContractContext contractContext,
                      UpdaterContext updaterContext) {
    this.innerContext = innerContext;
    this.stepsContext = stepsContext;
    this.contractContext = contractContext;
    this.updaterContext = updaterContext;
  }

  static GoalContext createGoalContext(TypeName goalType, BuildConfig config,
                                       ImmutableList<ProjectionInfo> projectionInfos,
                                       ExecutableElement goal, boolean toBuilder) {
    String builderTypeName = goalName(goalType, goal) + "Builder";
    ClassName builderType = config.generatedType.nestedClass(builderTypeName);
    ClassName contractType = builderType.nestedClass(CONTRACT);
    ImmutableList<ParameterContext> specs = specs(contractType, goalType, projectionInfos);
    SharedGoalContext sharedContext = new SharedGoalContext(goalType, builderType, config, toBuilder, goal, specs);
    return new GoalContext(sharedContext, new StepsContext(sharedContext),
        new ContractContext(sharedContext), new UpdaterContext(sharedContext));
  }

  private static ImmutableList<ParameterContext> specs(ClassName contractType, TypeName goalType,
                                                       ImmutableList<ProjectionInfo> goal) {
    ImmutableList.Builder<ParameterContext> stepSpecsBuilder = ImmutableList.builder();
    for (int i = goal.size() - 1; i >= 0; i--) {
      ProjectionInfo arg = goal.get(i);
      ClassName stepName = contractType.nestedClass(
          upcase(arg.parameter.getSimpleName().toString()));
      ParameterContext stepSpec = ParameterContext.stepSpec(stepName, arg, goalType);
      stepSpecsBuilder.add(stepSpec);
      goalType = stepSpec.stepContractType;
    }
    return stepSpecsBuilder.build().reverse();
  }

  static final class SharedGoalContext {

    /**
     * return type of {@link #goal} (or enclosing class in case of constructor)
     */
    final TypeName goalType;

    /**
     * container
     */
    final ClassName builderType;

    final BuildConfig config;

    final boolean toBuilder;

    /**
     * the element carrying the {@link net.zerobuilder.Build.Goal} annotation
     */
    final ExecutableElement goal;

    /**
     * arguments of the {@link #goal}
     */
    final ImmutableList<ParameterContext> stepSpecs;

    private SharedGoalContext(TypeName goalType, ClassName builderType, BuildConfig config,
                              boolean toBuilder, ExecutableElement goal,
                              ImmutableList<ParameterContext> stepSpecs) {
      this.goalType = goalType;
      this.builderType = builderType;
      this.config = config;
      this.toBuilder = toBuilder;
      this.goal = goal;
      this.stepSpecs = stepSpecs;
    }

    ImmutableList<ClassName> stepInterfaceNames() {
      ImmutableList.Builder<ClassName> specs = ImmutableList.builder();
      for (ParameterContext spec : stepSpecs) {
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

    ClassName contractName() {
      return builderType.nestedClass(CONTRACT);
    }

    ClassName stepsImplTypeName() {
      return builderType.nestedClass(STEPS_IMPL);
    }

    ClassName contractUpdaterName() {
      return contractName()
          .nestedClass(goalName() + UPDATER_SUFFIX);
    }

    CodeBlock goalParameters() {
      ImmutableList.Builder<CodeBlock> builder = ImmutableList.builder();
      for (VariableElement arg : goal.getParameters()) {
        builder.add(CodeBlock.of("$L", arg.getSimpleName()));
      }
      return joinCodeBlocks(builder.build(), ", ");
    }

    String goalName() {
      return goalName(goalType, goal);
    }

    String goalTypeName() {
      return ((ClassName) goalType.box()).simpleName();
    }

    static String goalName(TypeName goalType, ExecutableElement goal) {
      Build.Goal goalAnnotation = goal.getAnnotation(Build.Goal.class);
      if (goalAnnotation == null || isNullOrEmpty(goalAnnotation.name())) {
        return ((ClassName) goalType.box()).simpleName();
      }
      return upcase(goalAnnotation.name());
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

  TypeSpec builderImpl() {
    return classBuilder(innerContext.builderType)
        .addTypes(presentInstances(of(updaterContext.buildUpdaterImpl())))
        .addType(stepsContext.buildStepsImpl())
        .addType(contractContext.buildContract())
        .addModifiers(toArray(innerContext.maybeAddPublic(FINAL, STATIC), Modifier.class))
        .build();
  }

}
