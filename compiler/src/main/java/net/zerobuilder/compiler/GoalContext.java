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
import net.zerobuilder.Goal;
import net.zerobuilder.compiler.ToBuilderValidator.ValidParameter;

import javax.lang.model.element.ExecutableElement;
import javax.lang.model.element.Modifier;
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
                                       ImmutableList<ValidParameter> validParameters,
                                       ExecutableElement goal, boolean toBuilder,
                                       CodeBlock goalParameters) {
    String builderTypeName = goalName(goalType, goal) + "Builder";
    ClassName builderType = config.generatedType.nestedClass(builderTypeName);
    ClassName contractType = builderType.nestedClass(CONTRACT);
    ImmutableList<ParameterContext> parameters = parameters(contractType, goalType, validParameters);
    SharedGoalContext shared = new SharedGoalContext(goalType, builderType, config, toBuilder, goal, parameters, goalParameters);
    return new GoalContext(shared, new StepsContext(shared),
        new ContractContext(shared), new UpdaterContext(shared));
  }

  private static ImmutableList<ParameterContext> parameters(ClassName contract, TypeName returnType,
                                                            ImmutableList<ValidParameter> parameters) {
    ImmutableList.Builder<ParameterContext> builder = ImmutableList.builder();
    for (int i = parameters.size() - 1; i >= 0; i--) {
      ValidParameter parameter = parameters.get(i);
      ClassName stepContract = contract.nestedClass(
          upcase(parameter.name));
      builder.add(new ParameterContext(stepContract, parameter, returnType));
      returnType = stepContract;
    }
    return builder.build().reverse();
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
     * the element carrying the {@link Goal} annotation
     */
    final ExecutableElement goal;

    /**
     * blueprint for steps: parameters of the {@link #goal}, possibly in changed order
     */
    final ImmutableList<ParameterContext> parameters;

    /**
     * real parameters of the {@link #goal}
     */
    final CodeBlock goalParameters;

    private SharedGoalContext(TypeName goalType, ClassName builderType, BuildConfig config,
                              boolean toBuilder, ExecutableElement goal,
                              ImmutableList<ParameterContext> parameters, CodeBlock goalParameters) {
      this.goalType = goalType;
      this.builderType = builderType;
      this.config = config;
      this.toBuilder = toBuilder;
      this.goal = goal;
      this.parameters = parameters;
      this.goalParameters = goalParameters;
    }

    ImmutableList<ClassName> stepInterfaceNames() {
      ImmutableList.Builder<ClassName> specs = ImmutableList.builder();
      for (ParameterContext spec : parameters) {
        specs.add(spec.stepContract);
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

    String goalName() {
      return goalName(goalType, goal);
    }

    static String goalName(TypeName goalType, ExecutableElement goal) {
      Goal goalAnnotation = goal.getAnnotation(Goal.class);
      if (goalAnnotation == null || isNullOrEmpty(goalAnnotation.name())) {
        return goalTypeName(goalType);
      }
      return upcase(goalAnnotation.name());
    }

    String goalTypeName() {
      return goalTypeName(goalType);
    }

    private static String goalTypeName(TypeName goalType) {
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

  TypeSpec builderImpl() {
    return classBuilder(innerContext.builderType)
        .addTypes(presentInstances(of(updaterContext.buildUpdaterImpl())))
        .addType(stepsContext.buildStepsImpl())
        .addType(contractContext.buildContract())
        .addModifiers(toArray(innerContext.maybeAddPublic(FINAL, STATIC), Modifier.class))
        .build();
  }

}
