package net.zerobuilder.compiler;

import com.google.common.base.Function;
import com.google.common.collect.FluentIterable;
import com.google.common.collect.ImmutableList;
import com.squareup.javapoet.ClassName;
import com.squareup.javapoet.CodeBlock;
import com.squareup.javapoet.TypeName;
import com.squareup.javapoet.TypeSpec;
import net.zerobuilder.Goal;
import net.zerobuilder.compiler.ToBuilderValidator.ValidParameter;

import javax.lang.model.element.ExecutableElement;
import javax.lang.model.element.Modifier;
import javax.lang.model.type.TypeMirror;

import static com.google.common.base.Optional.presentInstances;
import static com.google.common.base.Strings.isNullOrEmpty;
import static com.google.common.collect.ImmutableList.of;
import static com.google.common.collect.Iterables.toArray;
import static com.squareup.javapoet.TypeSpec.classBuilder;
import static javax.lang.model.element.ElementKind.CONSTRUCTOR;
import static javax.lang.model.element.Modifier.FINAL;
import static javax.lang.model.element.Modifier.PUBLIC;
import static javax.lang.model.element.Modifier.STATIC;
import static net.zerobuilder.compiler.GoalContext.goalTypeName;
import static net.zerobuilder.compiler.Utilities.upcase;

final class UberGoalContext {

  static final String UPDATER_SUFFIX = "Updater";
  static final String CONTRACT = "Contract";
  static final String STEPS_IMPL = "StepsImpl";

  final GoalContext goal;
  final StepsContext stepsContext;
  final ContractContext contractContext;
  final UpdaterContext updaterContext;

  private UberGoalContext(GoalContext goal,
                          StepsContext stepsContext,
                          ContractContext contractContext,
                          UpdaterContext updaterContext) {
    this.goal = goal;
    this.stepsContext = stepsContext;
    this.contractContext = contractContext;
    this.updaterContext = updaterContext;
  }

  static UberGoalContext createGoalContext(TypeName goalType, BuildConfig config,
                                           ImmutableList<ValidParameter> validParameters,
                                           ExecutableElement goal, boolean toBuilder,
                                           CodeBlock goalParameters) {
    String builderTypeName = goalName(goalType, goal) + "Builder";
    ClassName builderType = config.generatedType.nestedClass(builderTypeName);
    ClassName contractType = builderType.nestedClass(CONTRACT);
    ImmutableList<ParameterContext> parameters = parameters(contractType, goalType, validParameters);
    GoalContext shared = new GoalContext(goalType, builderType, config, toBuilder,
        goalName(goalType, goal), goal.getSimpleName().toString(),
        goal.getKind() == CONSTRUCTOR
            ? GoalKind.CONSTRUCTOR
            : goal.getModifiers().contains(STATIC)
            ? GoalKind.STATIC_METHOD
            : GoalKind.INSTANCE_METHOD,
        goal.getModifiers().contains(PUBLIC)
            ? Visibility.PUBLIC
            : Visibility.PACKAGE,
        thrownTypes(goal), parameters, goalParameters);
    return new UberGoalContext(shared, new StepsContext(shared),
        new ContractContext(shared), new UpdaterContext(shared));
  }

  private static String goalName(TypeName goalType, ExecutableElement goal) {
    Goal goalAnnotation = goal.getAnnotation(Goal.class);
    if (goalAnnotation == null || isNullOrEmpty(goalAnnotation.name())) {
      return goalTypeName(goalType);
    }
    return upcase(goalAnnotation.name());
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

  enum GoalKind {
    CONSTRUCTOR, STATIC_METHOD, INSTANCE_METHOD
  }

  enum Visibility {
    PUBLIC, PACKAGE
  }

  TypeSpec builderImpl() {
    return classBuilder(goal.builderType)
        .addTypes(presentInstances(of(updaterContext.buildUpdaterImpl())))
        .addType(stepsContext.buildStepsImpl())
        .addType(contractContext.buildContract())
        .addModifiers(toArray(goal.maybeAddPublic(FINAL, STATIC), Modifier.class))
        .build();
  }

  private static ImmutableList<TypeName> thrownTypes(ExecutableElement goal) {
    return FluentIterable.from(goal.getThrownTypes())
        .transform(new Function<TypeMirror, TypeName>() {
          @Override
          public TypeName apply(TypeMirror thrownType) {
            return TypeName.get(thrownType);
          }
        })
        .toList();
  }
  
}
